package net.xdclass.dcloud_aipan.service;

import net.xdclass.dcloud_aipan.controller.req.FileChunkInitTaskReq;
import net.xdclass.dcloud_aipan.controller.req.FileChunkMergeReq;
import net.xdclass.dcloud_aipan.dto.FileChunkDTO;

public interface FileChunkService {
    /**
     * 初始化分片上传
     */
    FileChunkDTO initFileChunkTask(FileChunkInitTaskReq req);

    /**
     * 获取临时文件上传地址
     */
    String genPreSignUploadUrl(Long accountId, String identifier, Integer partNumber);

    /**
     * 合并分片
     */
    void mergeFileChunk(FileChunkMergeReq req);

    /**
     * 分片上传进度
     */
    FileChunkDTO listFileChunk(Long accountId, String identifier);
}
