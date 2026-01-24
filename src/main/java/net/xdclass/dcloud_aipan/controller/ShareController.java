package net.xdclass.dcloud_aipan.controller;

import net.xdclass.dcloud_aipan.dto.ShareDTO;
import net.xdclass.dcloud_aipan.service.ShareService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
