package net.xdclass.dcloud_aipan.controller;

import net.xdclass.dcloud_aipan.controller.req.ShareCancelReq;
import net.xdclass.dcloud_aipan.controller.req.ShareCreateReq;
import net.xdclass.dcloud_aipan.dto.ShareDTO;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.service.ShareService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/share/v1")
public class ShareController {

    @Autowired
    private ShareService shareService;


    /**
     * 获取我的个人分享列表接口
     */
    @GetMapping("list")
    public JsonData list() {
        List<ShareDTO> shareDTOList = shareService.listShare();
        return JsonData.buildSuccess(shareDTOList);
    }

    /**
     * 创建分享连接
     */
    @PostMapping("create")
    public JsonData create(@RequestBody ShareCreateReq req) {
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());

        ShareDTO shareDTO = shareService.createShare(req);

        return JsonData.buildSuccess(shareDTO);
    }

    /**
     * 取消分享
     */

    @PostMapping("cancel")
    public JsonData cancel(@RequestBody ShareCancelReq req) {

        req.setAccountId(LoginInterceptor.threadLocal.get().getId());

        shareService.cancelShare(req);

        return JsonData.buildSuccess();

    }
}
