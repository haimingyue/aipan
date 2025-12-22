package net.xdclass.dcloud_aipan.controller;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.controller.req.AccountLoginReq;
import net.xdclass.dcloud_aipan.controller.req.AccountRegisterReq;
import net.xdclass.dcloud_aipan.dto.AccountDTO;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.service.AccountService;
import net.xdclass.dcloud_aipan.util.JsonData;
import net.xdclass.dcloud_aipan.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("upload_avatar")
    public JsonData uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = accountService.uploadAvatar(file);
        return JsonData.buildSuccess(url);
    }

    @PostMapping("login")
    public JsonData login(@RequestBody AccountLoginReq req) {
        AccountDTO accountDTO = accountService.login(req);
        // 生成 jwt
        String token = JwtUtil.geneLoginJWT(accountDTO);
        return JsonData.buildSuccess(token);
    }

    /**
     * 获取用户详情接口
     */
    @GetMapping("detail")
    public JsonData detail() {
        AccountDTO accountDTO = accountService.queryDetail(LoginInterceptor.threadLocal.get().getId());
        return JsonData.buildSuccess(accountDTO);
    }
}
