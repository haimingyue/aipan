package net.xdclass.dcloud_aipan.controller;

import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.service.RecycleService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recycle/v1")
public class RecycleController {
    
    @Autowired
    RecycleService recycleService;
    
    /**
     * 获取回收站列表
     */
    @RequestMapping("list")
    public JsonData list(){
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        List<AccountFileDTO> list = recycleService.listRecycleFile(accountId);
        return JsonData.buildSuccess(list);
    }
}
