package net.xdclass.dcloud_aipan.controller;

import net.xdclass.dcloud_aipan.controller.req.ShareCancelReq;
import net.xdclass.dcloud_aipan.controller.req.ShareCheckReq;
import net.xdclass.dcloud_aipan.controller.req.ShareCreateReq;
import net.xdclass.dcloud_aipan.dto.ShareDTO;
import net.xdclass.dcloud_aipan.dto.ShareSimpleDTO;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
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

    /**
     * 访问分享接口
     * 情况 1：如果不需要校验码，则返回 token
     * 情况 2: 如果需要校验码，则返回校验码，调用对应的接口，再返回 token
     */
    @GetMapping("visit")
    public JsonData visit(@RequestParam(value = "shareId") Long shareId) {

        ShareSimpleDTO shareSimpleDTO = shareService.simpleDetail(shareId);

        return JsonData.buildSuccess(shareSimpleDTO);
    }

    /**
     * 校验分享码，返回 token
     */
    @PostMapping("check_share_code")
    public JsonData checkShareCode(@RequestBody ShareCheckReq shareCheckReq) {

        String shareToken = shareService.checkShareCode(shareCheckReq);

        if (shareToken == null) {
            return JsonData.buildResult(BizCodeEnum.SHARE_NOT_EXIST);
        }

        return JsonData.buildSuccess(shareToken);
    }
}
