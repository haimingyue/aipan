package net.xdclass.dcloud_aipan.service;

import net.xdclass.dcloud_aipan.dto.AccountFileDTO;

import java.util.List;

public interface FileService {
    /**
     * 获取文件列表
     */
    List<AccountFileDTO> listFile(Long accountId, Long parentId);
}
