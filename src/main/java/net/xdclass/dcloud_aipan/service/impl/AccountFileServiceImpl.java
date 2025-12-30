package net.xdclass.dcloud_aipan.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.component.StoreEngine;
import net.xdclass.dcloud_aipan.config.MinioConfig;
import net.xdclass.dcloud_aipan.controller.req.FileUpdateReq;
import net.xdclass.dcloud_aipan.controller.req.FileUploadReq;
import net.xdclass.dcloud_aipan.controller.req.FolderCreateReq;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.dto.FolderTreeNodeDTO;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.enums.FileTypeEnum;
import net.xdclass.dcloud_aipan.enums.FolderFlagEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.mapper.AccountFileMapper;
import net.xdclass.dcloud_aipan.mapper.FileMapper;
import net.xdclass.dcloud_aipan.model.AccountFileDO;
import net.xdclass.dcloud_aipan.model.FileDO;
import net.xdclass.dcloud_aipan.service.AccountFileService;
import net.xdclass.dcloud_aipan.util.CommonUtil;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountFileServiceImpl implements AccountFileService {

    @Autowired
    private AccountFileMapper accountFileMapper;

    @Autowired
    private StoreEngine fileStoreEngine;

    @Autowired
    MinioConfig minioConfig;
    @Autowired
    private FileMapper fileMapper;

    /**
     * 获取文件列表接口
     */
    @Override
    public List<AccountFileDTO> listFile(Long accountId, Long parentId) {
        List<AccountFileDO> accountFileDOList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .eq("parent_id", parentId)
                .orderByDesc("is_dir")
                .orderByDesc("gmt_create"));
        return SpringBeanUtil.copyProperties(accountFileDOList, AccountFileDTO.class);
    }

    @Override
    public Long createFolder(FolderCreateReq req) {
        AccountFileDTO accountFileDTO = AccountFileDTO.builder()
                .accountId(req.getAccountId())
                .parentId(req.getParentId())
                .fileName(req.getFolderName())
                .isDir(FolderFlagEnum.YES.getCode())
                .build();
        return saveAccountFile(accountFileDTO);
    }

    @Override
    public void renameFile(FileUpdateReq req) {
        AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("id", req.getFileId())
                .eq("account_id", req.getAccountId()));

        if (accountFileDO == null) {
            log.error("文件不存在");
            throw new BizException(BizCodeEnum.FILE_NOT_EXISTS);
        } else {
            // 新旧文件名不能一样
            if (Objects.equals(req.getNewFileName(), accountFileDO.getFileName())) {
                throw new BizException(BizCodeEnum.FILE_RENAME_REPEAT);
            }
            // 同层文件名不能一样
            Long selectCount = accountFileMapper.selectCount(new QueryWrapper<AccountFileDO>()
                    .eq("account_id", req.getAccountId())
                    .eq("parent_id", accountFileDO.getParentId())
                    .eq("file_name", req.getNewFileName()));
            if (selectCount > 0) {
                throw new BizException(BizCodeEnum.FILE_RENAME_REPEAT);
            }
            accountFileDO.setFileName(req.getNewFileName());
            accountFileMapper.updateById(accountFileDO);
        }
    }

    /**
     * 查询文件数接口
     * @return
     * 查询所有文件夹
     * 拼装文件树
     */
    @Override
    public List<FolderTreeNodeDTO> folderTree(Long accountId) {
        List<AccountFileDO> folderList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .eq("is_dir", FolderFlagEnum.YES.getCode()));

        if (CollectionUtil.isEmpty(folderList)) {
            return List.of();
        }
        // 构建一个 map 结构，key 是文件 id，value 是文件信息，相当于一个数据源
        Map<Long, FolderTreeNodeDTO> folderMap = folderList.stream()
                .collect(Collectors.toMap(AccountFileDO::getId, accountFileDO ->
                        FolderTreeNodeDTO.builder()
                                .id(accountFileDO.getId())
                                .parentId(accountFileDO.getParentId())
                                .label(accountFileDO.getFileName())
                                .children(new ArrayList<>())
                                .build()));

        // 构建文件树，遍历数据源，为每个文件夹找到其子文件夹
        for (FolderTreeNodeDTO node: folderMap.values()) {
            Long parentId = node.getParentId();
            if (parentId != null && folderMap.containsKey(parentId)) {
                // 获取父文件夹
                FolderTreeNodeDTO parentNode = folderMap.get(parentId);
                List<FolderTreeNodeDTO> children = parentNode.getChildren();
                children.add(node);
            }
        }

        // 过滤根节点，即parent_id = 0
        return folderMap.values().stream().filter(node -> Objects.equals(node.getParentId(), 0L)).collect(Collectors.toList());


//        return rootFolderList;
    }

    @Override
    public List<FolderTreeNodeDTO> folderTreeV2(Long accountId) {
        List<AccountFileDO> folderList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .eq("is_dir", FolderFlagEnum.YES.getCode()));

        if (CollectionUtil.isEmpty(folderList)) {
            return List.of();
        }

        List<FolderTreeNodeDTO> folderTreeNodeDTOList = folderList.stream().map(file -> {
            return FolderTreeNodeDTO.builder()
                    .id(file.getId())
                    .parentId(file.getParentId())
                    .label(file.getFileName())
                    .children(new ArrayList<>())
                    .build();
        }).toList();

        // 根据父文件 ID 分组 key 是当前文件夹 id，value 是子文件夹列表
        Map<Long, List<FolderTreeNodeDTO>> parentIdGroup = folderTreeNodeDTOList
                .stream().collect(Collectors.groupingBy(FolderTreeNodeDTO::getParentId));

        for (FolderTreeNodeDTO node: folderTreeNodeDTOList) {
            List<FolderTreeNodeDTO> children = parentIdGroup.get(node.getId());
            // 判断是否为空
            if (!CollectionUtil.isEmpty(children)) {
                node.getChildren().addAll(children);
            }
        }

        return folderTreeNodeDTOList
                .stream().filter(node -> Objects.equals(node.getParentId(), 0L)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fileUpload(FileUploadReq req) {
        // 1. 上传到存储引擎
        String storeFileObjectKey = storeFile(req);
        // 2. 保存文件信息
        // 3. 保存账号和文件关系
        setFileAndAccountFile(req, storeFileObjectKey);

    }

    private void setFileAndAccountFile(FileUploadReq req, String storeFileObjectKey) {
        FileDO fileDO = saveFile(req, storeFileObjectKey);
        AccountFileDTO accountFileDTO = AccountFileDTO.builder()
                .accountId(req.getAccountId())
                .parentId(req.getParentId())
                .fileId(fileDO.getId())
                .fileName(req.getFilename())
                .isDir(FolderFlagEnum.NO.getCode())
                .fileSuffix(fileDO.getFileSuffix())
                .fileSize(fileDO.getFileSize())
                .fileType(FileTypeEnum.fromExtension(fileDO.getFileSuffix()).name())
                .build();

        saveAccountFile(accountFileDTO);
    }

    private FileDO saveFile(FileUploadReq req, String storeFileObjectKey) {
        FileDO fileDO = new FileDO();
        fileDO.setAccountId(req.getAccountId());
        fileDO.setFileName(req.getFilename());
        fileDO.setFileSize(req.getFile() != null ? req.getFile().getSize() :req.getFileSize());
        fileDO.setFileSuffix(CommonUtil.getFileSuffix(req.getFilename()));
        fileDO.setObjectKey(storeFileObjectKey);
        fileDO.setIdentifier(req.getIdentifier());
        fileMapper.insert(fileDO);
        return fileDO;


    }

    /**
     * 上传文件到存储引擎，返回文件路径
     */
    private String storeFile(FileUploadReq req) {
        String objectKey = CommonUtil.getFilePath(req.getFilename());
        fileStoreEngine.upload(minioConfig.getBucketName(), objectKey, req.getFile());
        return objectKey;
    }

    private Long saveAccountFile(AccountFileDTO accountFileDTO) {
        // 检查父文件是否存在
        checkAccountFileId(accountFileDTO);
        // 检查文件是否重复 aa aa(1) aa(2)
        AccountFileDO accountFileDO = SpringBeanUtil.copyProperties(accountFileDTO, AccountFileDO.class);
        processAccountFileDuplicate(accountFileDO);
        // 保存相关文件关系
        accountFileMapper.insert(accountFileDO);
        return accountFileDO.getId();
    }

    /**
     * 文件名是否存在
     */
    private void processAccountFileDuplicate(AccountFileDO accountFileDO) {
        Long selectCount = accountFileMapper.selectCount(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountFileDO.getAccountId())
                .eq("parent_id", accountFileDO.getParentId())
                .eq("is_dir", accountFileDO.getIsDir())
                .eq("file_name", accountFileDO.getFileName()));
        if (selectCount > 0) {
            // 处理文件夹
            if (Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
                accountFileDO.setFileName(accountFileDO.getFileName() + "_" + System.currentTimeMillis());
            } else {
                // 处理文件：后续可按需求添加文件重名处理策略
                // 提前文件拓展名
                String[] split = accountFileDO.getFileName().split("\\.");
                String fileName = split[0];
                String fileSuffix = split[1];
                accountFileDO.setFileName(fileName + "_" + System.currentTimeMillis() + "." + fileSuffix);
            }
        }
    }

    private void checkAccountFileId(AccountFileDTO accountFileDTO) {
        Long parentId = accountFileDTO.getParentId();
        if (parentId != 0) {
            AccountFileDO parentFile = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                    .eq("id", parentId)
                    .eq("account_id", accountFileDTO.getAccountId()));
            if (parentFile == null) {
                throw new BizException(BizCodeEnum.FILE_NOT_EXISTS );
            }
        }
    }
}
