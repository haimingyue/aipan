package net.xdclass.dcloud_aipan.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.component.StoreEngine;
import net.xdclass.dcloud_aipan.config.MinioConfig;
import net.xdclass.dcloud_aipan.controller.req.*;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.dto.FolderTreeNodeDTO;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.enums.FileTypeEnum;
import net.xdclass.dcloud_aipan.enums.FolderFlagEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.mapper.AccountFileMapper;
import net.xdclass.dcloud_aipan.mapper.FileMapper;
import net.xdclass.dcloud_aipan.mapper.StorageMapper;
import net.xdclass.dcloud_aipan.model.AccountFileDO;
import net.xdclass.dcloud_aipan.model.FileDO;
import net.xdclass.dcloud_aipan.model.StorageDO;
import net.xdclass.dcloud_aipan.service.AccountFileService;
import net.xdclass.dcloud_aipan.util.CommonUtil;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
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

    @Autowired
    private StorageMapper storageMapper;

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

        boolean storeEnough = checkAndUpdateCapacity(req.getAccountId(), req.getFileSize());

        if (storeEnough) {
            // 1. 上传到存储引擎
            String storeFileObjectKey = storeFile(req);
            // 2. 保存文件信息
            // 3. 保存账号和文件关系
            setFileAndAccountFile(req, storeFileObjectKey);
        } else {
            throw new BizException(BizCodeEnum.FILE_STORAGE_NOT_ENOUGH);
        }


    }

    private boolean checkAndUpdateCapacity(Long accountId, Long fileSize) {
        StorageDO storageDO = storageMapper.selectOne(new QueryWrapper<StorageDO>().eq("account_id", accountId));

        Long totalSize = storageDO.getTotalSize();

        if (storageDO.getUsedSize() + fileSize <= totalSize) {
            storageDO.setUsedSize(storageDO.getUsedSize() + fileSize);
            storageMapper.updateById(storageDO);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 文件批量移动
     * 1. 检查被移动的文件 id 是否合法
     * 2. 检查目标文件夹id 是否合法
     * 3. 批量移动文件到目标文件夹(重复名称处理)
     */
    @Override
    public void moveBatch(FileBatchReq req) {
        // 1. 检查被移动的文件 id 是否合法
        List<AccountFileDO> accountFileDOList = checkFileIdLegal(req.getFileIds(), req.getAccountId());

        // 2. 检查目标文件夹id 是否合法, 包括子文件夹
        checkTargetParentIdLegal(req);

        accountFileDOList.forEach(accountFileDO -> {
            accountFileDO.setParentId(req.getTargetParentId());
        });

        // 3. 批量移动文件到目标文件夹(重复名称处理)
        accountFileDOList.forEach(this::processAccountFileDuplicate);

        // 4. 更新文件或者文件夹 parentId 为 目标文件夹id
//        UpdateWrapper<AccountFileDO> updateWrapper = new UpdateWrapper<>();
//        updateWrapper.in("id", req.getFileIds())
//                .set("parent_id", req.getTargetParentId());
//        int updateCount = accountFileMapper.update(null, updateWrapper);
//        if (updateCount != accountFileDOList.size()) {
//            throw new BizException(BizCodeEnum.FILE_BATCH_UPDATE_ERROR);
//        }
        for (AccountFileDO accountFileDO: accountFileDOList) {
            if (accountFileMapper.updateById(accountFileDO) < 0) {
                throw new BizException(BizCodeEnum.FILE_BATCH_UPDATE_ERROR);
            }
        }
    }

    /**
     * 检查目标文件夹id 是否合法, 包括子文件夹
     * 1. 目标文件 ID 不能是文件
     * 2. 要操作的文件列表不能包括目标文件 ID
     */
    private void checkTargetParentIdLegal(FileBatchReq req) {
        AccountFileDO targetAccountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("id", req.getTargetParentId())
                .eq("is_dir", FolderFlagEnum.YES.getCode())
                .eq("account_id", req.getAccountId()));
        if (targetAccountFileDO == null) {
            log.error("目标文件不能是文件，需要是文件夹, targetParentId={}", req.getTargetParentId());
            throw new BizException(BizCodeEnum.FILE_BATCH_UPDATE_ERROR);
        }

        List<AccountFileDO> prepareAccountFileDOS = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", req.getFileIds())
                .eq("account_id", req.getAccountId()));

        // 定义一个容器存储全部文件夹，包括子文件夹
        List<AccountFileDO> allAccountFileDOList = new ArrayList<>();
        findAllAccountFileDOWithRecur(allAccountFileDOList, prepareAccountFileDOS, false);

        if (allAccountFileDOList.stream().anyMatch(accountFileDO -> accountFileDO.getId().equals(req.getTargetParentId()))) {
            log.error("目标文件夹不能是操作的文件夹，不能包含子文件夹, targetParentId={}", req.getTargetParentId());
            throw new BizException(BizCodeEnum.FILE_BATCH_UPDATE_ERROR);
        }
    }

    private void findAllAccountFileDOWithRecur(List<AccountFileDO> allAccountFileDOList, List<AccountFileDO> prepareAccountFileDOS, boolean onlyFolder) {
        for (AccountFileDO accountFileDO : prepareAccountFileDOS) {
            if (Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
                List<AccountFileDO> childAccountFileDOList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                        .eq("parent_id", accountFileDO.getId()));
                // 递归查找
                findAllAccountFileDOWithRecur(allAccountFileDOList, childAccountFileDOList, onlyFolder);
            }

            // 存储相关内容，根据 onlyfolde 操作
            if (!onlyFolder || Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
                allAccountFileDOList.add(accountFileDO);
            }

        }
    }

    /**
     * 检查文件 id 是否合法
     */
    public List<AccountFileDO> checkFileIdLegal(List<Long> fileIds, Long accountId) {
        List<AccountFileDO> accountFileDOList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .in("id", fileIds));
        if (accountFileDOList.size() != fileIds.size()) {
            log.error("文件ID数量不合法, ids={}", fileIds );
            throw new BizException(BizCodeEnum.FILE_BATCH_UPDATE_ERROR);
        }
        return accountFileDOList;
    }

    @Override
    public void setFileAndAccountFile(FileUploadReq req, String storeFileObjectKey) {
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


    /**
     * 批量删除文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delBatch(FileDelReq req) {
        // 步骤 1: 检查是否满足：文件 ID数量是否合法 2. 文件是否属于当前用户
        List<AccountFileDO> accountFileDOList = checkFileIdLegal(req.getFileIds(), req.getAccountId());
        // 步骤 2：判断文件是否是文件夹，文件夹的话需要递归获取里面子文件 ID，然后进行批量删除
        List<AccountFileDO> storeAccountFileDOList = new ArrayList<>();
        findAllAccountFileDOWithRecur(storeAccountFileDOList, accountFileDOList, false);
        // 拿到全部文件 ID，含文件夹
        List<Long> allFileIdList = storeAccountFileDOList.stream().map(AccountFileDO::getId).collect(Collectors.toList());
        // 步骤 3: 需要更新账号存储空间
//        long allFileSize = storeAccountFileDOList.stream().filter(file -> file.getIsDir().equals(FolderFlagEnum.NO.getCode()))
//                .mapToLong(AccountFileDO::getFileSize).sum();
        long allFileSize = storeAccountFileDOList.stream()
                .filter(file -> file.getIsDir().equals(FolderFlagEnum.NO.getCode()))
                .mapToLong(AccountFileDO::getFileSize).sum();

        StorageDO storageDO = storageMapper.selectOne(new QueryWrapper<StorageDO>().eq("account_id", req.getAccountId()));
        storageDO.setUsedSize(storageDO.getUsedSize() - allFileSize);
        storageMapper.updateById(storageDO);
        // 步骤 4：批量删除账号映射文件，考虑回收站如何设计
        accountFileMapper.deleteBatchIds(allFileIdList);
    }

    /**
     * 1. 检查被转移的 ID 是否合法
     * 2. 检查目标 ID 是否合法
     * 3. 执行拷贝，递归查找【差异点，ID 是全新的】
     * 4. 计算存储空间，是否足够
     * 5. 存储相关结构
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyBatch(FileBatchReq req) {
        // 1. 检查被转移的 ID 是否合法
        List<AccountFileDO> accountFileDOList = checkFileIdLegal(req.getFileIds(), req.getAccountId());
        // 2. 检查目标 ID 是否合法
        checkTargetParentIdLegal(req);
        // 3. 执行拷贝，递归查找【差异点，ID 是全新的】
        List<AccountFileDO> newAccountFileDO = findBatchCopyWithRecur(accountFileDOList, req.getTargetParentId());
        // 4. 计算存储空间
        long totalFileSize = newAccountFileDO.stream().filter(file -> file.getIsDir().equals(FolderFlagEnum.NO.getCode()))
                .mapToLong(AccountFileDO::getFileSize).sum();
        if (!checkAndUpdateCapacity(req.getAccountId(), totalFileSize)) {
            throw new BizException(BizCodeEnum.FILE_STORAGE_NOT_ENOUGH);
        }
        accountFileMapper.insertFileBatch(newAccountFileDO);
    }

    /**
     * 文件秒传
     * 1. 检查文件是否存在
     * 2. 检查空间是否足够
     * 3. 建立关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean secondUpload(FileSecondUploadReq req) {
        FileDO fileDO = fileMapper.selectOne(new QueryWrapper<FileDO>().eq("identifier", req.getIdentifier()));

        if (fileDO != null && checkAndUpdateCapacity(req.getAccountId(), fileDO.getFileSize())) {
            AccountFileDTO accountFileDTO = new AccountFileDTO();
            accountFileDTO.setAccountId(req.getAccountId());
            accountFileDTO.setFileId(fileDO.getId());
            accountFileDTO.setParentId(req.getParentId());
            accountFileDTO.setFileName(req.getFilename());
            // size
            accountFileDTO.setFileSize(fileDO.getFileSize());
            // del
            accountFileDTO.setDel(false);
            // dir
            accountFileDTO.setIsDir(FolderFlagEnum.NO.getCode());

            // 保存关联关系
            saveAccountFile(accountFileDTO);
            return true;
        }
        return false;
    }

    private List<AccountFileDO> findBatchCopyWithRecur(List<AccountFileDO> accountFileDOList, Long targetParentId) {
        List<AccountFileDO> newAccountFileDO = new ArrayList<>();

        accountFileDOList.forEach(accountFileDO -> doCopyFileRecur(newAccountFileDO, accountFileDO, targetParentId));

        return newAccountFileDO;
    }

    /**
     * 递归处理，包括子文件夹
     */
    private void doCopyFileRecur(List<AccountFileDO> newAccountFileDO, AccountFileDO accountFileDO, Long targetParentId) {
        // 保存旧的文件 id，方便查找子文件夹
        Long oldAccountFileId = accountFileDO.getId();
        // 创建新记录
        accountFileDO.setId(IdUtil.getSnowflakeNextId());
        accountFileDO.setParentId(targetParentId);
        accountFileDO.setGmtModified(null);
        accountFileDO.setGmtCreate(null);

        // 处理重复文件名
        processAccountFileDuplicate(accountFileDO);

        // 纳入容器存储
        newAccountFileDO.add(accountFileDO);

        if (Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
            // 继续获取子文件列表
            List<AccountFileDO> childAccountFileDOList = findChildAccountFile(accountFileDO.getAccountId(), oldAccountFileId);
            if (CollectionUtils.isEmpty(childAccountFileDOList)) {
                return;
            }
            // 递归处理
            childAccountFileDOList
                    .forEach(childAccountFileDO -> doCopyFileRecur(newAccountFileDO, childAccountFileDO, accountFileDO.getId()));
        }

    }

    /**
     * 查找文件记录，查询下一级，不递归
     */
    private List<AccountFileDO> findChildAccountFile(Long accountId, Long parentId) {
        return accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .eq("parent_id", parentId));
    }
}
