package net.xdclass.dcloud_aipan.controller.req;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

@Data
@Accessors(chain = true)
public class FileUploadReq {
    // 文件名称
    private String filename;
    // 文件标识符，用于分片上传等场景
    private String identifier;
    // 账户ID，标识上传文件的用户
    private Long accountId;
    // 父目录ID，标识文件存储的目录
    private Long parentId;
    // 文件大小
    private Long fileSize;
    // 上传的文件对象
    private MultipartFile file;
}
