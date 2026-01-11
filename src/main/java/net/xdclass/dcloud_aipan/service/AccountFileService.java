package net.xdclass.dcloud_aipan.service;

import net.xdclass.dcloud_aipan.controller.req.*;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.dto.FolderTreeNodeDTO;

import java.util.List;

public interface AccountFileService {
    /**
     * 获取文件列表
     */
    List<AccountFileDTO> listFile(Long accountId, Long parentId);

    Long createFolder(FolderCreateReq req);

    void renameFile(FileUpdateReq req);

    List<FolderTreeNodeDTO> folderTree(Long accountId);

    List<FolderTreeNodeDTO> folderTreeV2(Long accountId);

    void fileUpload(FileUploadReq req);

    void moveBatch(FileBatchReq req);

    void delBatch(FileDelReq req);

    void copyBatch(FileBatchReq req);

    Boolean secondUpload(FileSecondUploadReq req);
}
