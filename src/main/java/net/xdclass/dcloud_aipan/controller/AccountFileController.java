package net.xdclass.dcloud_aipan.controller;

import net.xdclass.dcloud_aipan.controller.req.*;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.dto.FileChunkDTO;
import net.xdclass.dcloud_aipan.dto.FolderTreeNodeDTO;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.service.AccountFileService;
import net.xdclass.dcloud_aipan.service.FileChunkService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/file/v1")
public class AccountFileController {

    @Autowired
    private AccountFileService accountFileService;
    @Autowired
    private FileChunkService fileChunkService;

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

    /**
     * 文件重命名
     */
    @PostMapping("rename_file")
    public Object renameFile(@RequestBody FileUpdateReq req){
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        req.setAccountId(accountId);

        accountFileService.renameFile(req);
        return JsonData.buildSuccess();
    }

    /**
     * 查询文件树接口
     */
    @GetMapping("/folder/tree")
    public Object folderTree(){
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        List<FolderTreeNodeDTO> list = accountFileService.folderTree(accountId);
        return JsonData.buildSuccess(list);
    }

    /**
     * 普通小文件上传
     */
    @PostMapping("upload")
    public JsonData upload(FileUploadReq req){
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        accountFileService.fileUpload(req);
        return JsonData.buildSuccess();
    }

    /**
     * 文件批量移动
     */
    @PostMapping("move_batch")
    public JsonData moveBatch(@RequestBody FileBatchReq req){
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        accountFileService.moveBatch(req);
        return JsonData.buildSuccess();
    }

    /**
     * 文件批量删除
     */
    @PostMapping("del_batch")
    public JsonData delBatch(@RequestBody FileDelReq req){
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        accountFileService.delBatch(req);
        return JsonData.buildSuccess();
    }

    /**
     * 文件复制
     */
    @PostMapping("copy_batch")
    public JsonData copyBatch(@RequestBody FileBatchReq req){
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        accountFileService.copyBatch(req);
        return JsonData.buildSuccess();
    }

    /**
     * 文件秒传接口
     * true 就是文件秒传成功
     * false 就会失败，需要重新调用上传接口
     */
    @PostMapping("second_upload")
    public JsonData secondUpload(@RequestBody FileSecondUploadReq req){
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        Boolean flag =  accountFileService.secondUpload(req);
        return JsonData.buildSuccess(flag);
    }

    /**
     * 1. 创建分片上传任务
     */
    @PostMapping("init_upload")
    public JsonData initFileChunkTask(@RequestBody FileChunkInitTaskReq req) {
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        FileChunkDTO fileChunkDTO = fileChunkService.initFileChunkTask(req);
        return JsonData.buildSuccess(fileChunkDTO);
    }

    /**
     * 2. 获取分片上传地址，返回 minio 临时签名地址
     */
    @GetMapping("get_file_chunk_upload_url/{identifier}/{partNumber}")
    public JsonData getFileChunkUploadUrl(@PathVariable("identifier") String identifier,
                                          @PathVariable("partNumber") Integer partNumber) {
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        String url = fileChunkService.genPreSignUploadUrl(accountId, identifier, partNumber);
        return JsonData.buildSuccess(url);
    }

    /**
     * 3. 合并分片
     */
    @PostMapping("merge_file_chunk")
    public JsonData mergeFileChunk(@RequestBody FileChunkMergeReq req) {
        // 获取登录 id
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        req.setAccountId(accountId);
        fileChunkService.mergeFileChunk(req);
        return JsonData.buildSuccess();
    }

    /**
     * 4. 查询分片进度
     */
    @GetMapping("chunk_upload_progress/{identifier}")
    public JsonData chunkUploadProgress(@PathVariable("identifier") String identifier) {
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        FileChunkDTO fileChunkDTO = fileChunkService.listFileChunk(accountId, identifier);
        return JsonData.buildSuccess(fileChunkDTO);
    }
}
