package net.xdclass.dcloud_aipan.controller;

import net.xdclass.dcloud_aipan.controller.req.RecycleDeleteReq;
import net.xdclass.dcloud_aipan.controller.req.RecycleRestoreReq;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.service.RecycleService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recycle/v1")
public class RecycleController {
    
    @Autowired
    RecycleService recycleService;
    
    /**
     * 获取回收站列表
     */
    @GetMapping("list")
    public JsonData list(){
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        List<AccountFileDTO> list = recycleService.listRecycleFile(accountId);
        return JsonData.buildSuccess(list);
    }

    /**
     * 彻底删除回收站
     */
    @PostMapping("delete")
    public JsonData delete(@RequestBody RecycleDeleteReq req){
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        recycleService.delete(req);
        return JsonData.buildSuccess();
    }

    /**
     * 还原回收站文件 - 支持多种路径格式
     */
    @PostMapping(value = {"/restore", "restore"}, produces = "application/json;charset=UTF-8")
    public JsonData restore(@RequestBody RecycleRestoreReq req){
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        recycleService.restore(req);
        return JsonData.buildSuccess();
    }
}
