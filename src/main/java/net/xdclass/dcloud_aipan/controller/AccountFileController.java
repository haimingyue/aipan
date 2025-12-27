package net.xdclass.dcloud_aipan.controller;

import net.xdclass.dcloud_aipan.controller.req.FolderCreateReq;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.service.AccountFileService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/file/v1")
public class AccountFileController {

    @Autowired
    private AccountFileService accountFileService;

    /**
     * 查询文件列表
     */
    @GetMapping("list")
    public Object list(@RequestParam(value = "parent_id")Long parentId){
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        List<AccountFileDTO> accountFileList = accountFileService.listFile(accountId, parentId);
        return JsonData.buildSuccess(accountFileList);
    }

    /**
     * 创建文件夹
     */
    @PostMapping("create_folder")
    public Object createFolder(@RequestBody FolderCreateReq req){
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        req.setAccountId(accountId);

        accountFileService.createFolder(req);
        return JsonData.buildSuccess();
    }
}
