package net.xdclass.dcloud_aipan.service;

import net.xdclass.dcloud_aipan.controller.req.AccountLoginReq;
import net.xdclass.dcloud_aipan.controller.req.AccountRegisterReq;
import net.xdclass.dcloud_aipan.dto.AccountDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AccountService {


    void register(AccountRegisterReq req);

    String uploadAvatar(MultipartFile file);

    AccountDTO login(AccountLoginReq req);

    AccountDTO queryDetail(Long id);
}
