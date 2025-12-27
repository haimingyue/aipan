package net.xdclass.dcloud_aipan.controller;

import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.service.FileService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/file/v1")
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 查询文件列表
     */
    @GetMapping("list")
    public Object list(@RequestParam(value = "parent_id")Long parentId){
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        List<AccountFileDTO> accountFileList = fileService.listFile(accountId, parentId);
        return JsonData.buildSuccess(accountFileList);
    }
}
