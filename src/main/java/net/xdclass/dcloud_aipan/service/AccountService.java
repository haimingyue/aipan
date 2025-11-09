package net.xdclass.dcloud_aipan.service;

import net.xdclass.dcloud_aipan.controller.req.AccountRegisterReq;
import org.springframework.web.multipart.MultipartFile;

public interface AccountService {
    void register(AccountRegisterReq req);

    String uploadAvatar(MultipartFile file);
}
