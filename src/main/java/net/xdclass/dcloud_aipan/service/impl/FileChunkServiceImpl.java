package net.xdclass.dcloud_aipan.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.xdclass.dcloud_aipan.component.StoreEngine;
import net.xdclass.dcloud_aipan.config.MinioConfig;
import net.xdclass.dcloud_aipan.controller.req.FileChunkInitTaskReq;
import net.xdclass.dcloud_aipan.controller.req.FileChunkMergeReq;
import net.xdclass.dcloud_aipan.controller.req.FileUploadReq;
import net.xdclass.dcloud_aipan.dto.FileChunkDTO;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.mapper.FileChunkMapper;
import net.xdclass.dcloud_aipan.mapper.StorageMapper;
import net.xdclass.dcloud_aipan.model.FileChunkDO;
import net.xdclass.dcloud_aipan.model.StorageDO;
import net.xdclass.dcloud_aipan.service.AccountFileService;
import net.xdclass.dcloud_aipan.service.FileChunkService;
import net.xdclass.dcloud_aipan.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileChunkServiceImpl implements FileChunkService {

    @Autowired
    private StorageMapper storageMapper;

    @Autowired
    private StoreEngine fileStorageEngine;

    @Autowired
    private FileChunkMapper fileChunkMapper;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private AccountFileService accountFileService;


    /**
     * 检查文件空间是否够
     * 根据文件名推断文件内容
     * 初始化分片上传
     * 创建上传任务实体并设置相关属性
     * 将任务插入数据库，构建并返回任务信息 DTO
     */
    @Override
    public FileChunkDTO initFileChunkTask(FileChunkInitTaskReq req) {


        StorageDO storageDO = storageMapper.selectOne(new QueryWrapper<>(new StorageDO()).eq("accountId", req.getAccountId()));

        if (storageDO.getUsedSize() + req.getTotalSize() > storageDO.getTotalSize()) {
            throw new BizException(BizCodeEnum.FILE_STORAGE_NOT_ENOUGH);
        }

        String objectKey = CommonUtil.getFilePath(req.getFilename());

        // 获取文件类型
        String contentType = MediaTypeFactory.getMediaType(objectKey).orElse(MediaType.APPLICATION_OCTET_STREAM).toString();

        // 配置一下元数据
        ObjectMetadata metadata = new ObjectMetadata();

        metadata.setContentType(contentType);

        // 初始化分片上传
        InitiateMultipartUploadResult uploadResult = fileStorageEngine.initMultipartUploadTask(minioConfig.getBucketName(), objectKey, metadata);
        String uploadId = uploadResult.getUploadId();

        int fileChunkNum = (int)Math.ceil(req.getTotalSize() * 1.0 / req.getChunkSize());
        FileChunkDO task = new FileChunkDO();
        task.setBucketName(minioConfig.getBucketName())
                .setChunkNum(fileChunkNum)
                .setChunkSize(req.getChunkSize())
                .setTotalSize(req.getTotalSize())
                .setFileName(req.getFilename())
                .setIdentifier(req.getIdentifier())
                .setObjectKey(objectKey)
                .setUploadId(uploadId)
                .setAccountId(req.getAccountId());

        // 保存到数据库
        fileChunkMapper.insert(task);

        return new FileChunkDTO(task).setFinished(false).setExistPartList(new ArrayList<>());
    }

    @Override
    public String genPreSignUploadUrl(Long accountId, String identifier, Integer partNumber) {
        FileChunkDO task = fileChunkMapper.selectOne(new QueryWrapper<FileChunkDO>()
                .eq("accountId", accountId)
                .eq("identifier", identifier));
        if (task == null) {
            throw new BizException(BizCodeEnum.FILE_CHUNK_TASK_NOT_EXISTS);
        }

        // 配置预签名时间
        DateTime expireTime = DateUtil.offsetMillisecond(new Date(), minioConfig.getPreSignUrlExpireTime().intValue());

        // 生成签名 URL
        Map<String, Object> params = new HashMap<>();
        params.put("partNumber", partNumber);
        params.put("uploadId", task.getUploadId());
        URL preSignUrl = fileStorageEngine.genePreSignedUrl(minioConfig.getBucketName(), task.getObjectKey(), HttpMethod.PUT, expireTime, params);
        return preSignUrl.toString();
    }

    /**
     * 获取任务和分片列表，检查是否足够合并
     * 检查存储空间和更新
     * 合并分片
     * 判断合并分片是否成功
     * 存储分片和关联信息到数据库
     * 根据唯一标识符删除相关分片信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeFileChunk(FileChunkMergeReq  req) {
        // 获取任务和分片列表，检查是否足够合并
        FileChunkDO task = fileChunkMapper.selectOne(new QueryWrapper<FileChunkDO>()
                .eq("accountId", req.getAccountId())
                .eq("identifier", req.getIdentifier()));

        if (task == null) {
            throw new BizException(BizCodeEnum.FILE_CHUNK_TASK_NOT_EXISTS);
        }

        PartListing partListing = fileStorageEngine.listMultipart(task.getBucketName(), task.getObjectKey(), task.getUploadId());
        List<PartSummary> parts = partListing.getParts();
        if (parts.size() != task.getChunkNum()) {
            throw new BizException(BizCodeEnum.FILE_CHUNK_NOT_ENOUGH);
        }

        long totalSize = parts.stream().map(PartSummary::getSize).mapToLong( Long::intValue).sum();
        // 如果一样，检查更新存储空间
        StorageDO storageDO = storageMapper.selectOne(new QueryWrapper<StorageDO>().eq("accountId", req.getAccountId()));
        if (storageDO.getUsedSize() + totalSize > storageDO.getTotalSize()) {
            throw new BizException(BizCodeEnum.FILE_STORAGE_NOT_ENOUGH);
        }
        storageDO.setUsedSize(storageDO.getUsedSize() + totalSize);
        storageMapper.updateById(storageDO);

        // 2- 合并文件
        CompleteMultipartUploadResult result = fileStorageEngine.mergeChunks(task.getBucketName(), task.getObjectKey(), task.getUploadId()
                , parts.stream().map(part -> new PartETag(part.getPartNumber(), part.getETag())).collect(Collectors.toList()));

        if (result.getETag() != null) {
            FileUploadReq fileUploadReq = new FileUploadReq();
            fileUploadReq.setAccountId(req.getAccountId())
                    .setParentId(req.getParentId())
                    .setFilename(task.getFileName())
                    .setFileSize(totalSize)
                    .setIdentifier(task.getIdentifier())
                    .setFile(null);

            // 存储文件和关联信息到数据库文件
            accountFileService.setFileAndAccountFile(fileUploadReq, task.getObjectKey());

            fileChunkMapper.deleteById(task.getId() );
        }
    }

    @Override
    public FileChunkDTO listFileChunk(Long accountId, String identifier) {
        // 1. 查询任务是否存在？
        FileChunkDO task = fileChunkMapper.selectOne(new QueryWrapper<FileChunkDO>()
                .eq("accountId", accountId)
                .eq("identifier", identifier));
        if (task == null) {
            throw new BizException(BizCodeEnum.FILE_CHUNK_TASK_NOT_EXISTS);
        }

        FileChunkDTO result = new FileChunkDTO(task);
        boolean objectExist = fileStorageEngine.doesObjectExist(task.getBucketName(), task.getObjectKey());
        if (!objectExist) {
            // 不存在就是未上传，返回已上传的分片概述
            PartListing partListing = fileStorageEngine.listMultipart(task.getBucketName(), task.getObjectKey(), task.getUploadId());
            if (partListing.getParts().size() == task.getChunkNum()) {
                result.setFinished(true).setExistPartList(partListing.getParts());
            } else {
                // 未上传完成，还不能合并
                result.setFinished(false).setExistPartList(partListing.getParts());
            }
        }
        return result;
    }
}
