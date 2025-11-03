package net.xdclass.dcloud_aipan.controller;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.controller.req.AccountRegisterReq;
import net.xdclass.dcloud_aipan.service.AccountService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/account/v1")
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * 注册接口 JSONData
     */
    @PostMapping("register")
    public JsonData register(@RequestBody AccountRegisterReq  req) {
        accountService.register(req);
        return JsonData.buildSuccess();
    }

}
