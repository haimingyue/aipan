package net.xdclass.dcloud_aipan.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.controller.req.FolderCreateReq;
import net.xdclass.dcloud_aipan.mapper.AccountFileMapper;
import net.xdclass.dcloud_aipan.mapper.FileMapper;
import net.xdclass.dcloud_aipan.service.AccountFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountFileServiceImpl implements AccountFileService {

    @Autowired
    private AccountFileMapper accountFileMapper;
    @Autowired
    private FileMapper fileMapper;


    @Override
    public void createFolder(FolderCreateReq rootFolderReq) {

    }
}
