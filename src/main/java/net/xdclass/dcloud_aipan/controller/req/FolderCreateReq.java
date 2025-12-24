package net.xdclass.dcloud_aipan.controller.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "FolderCreateReq", description = "新建文件夹请求参数")
public class FolderCreateReq {
    /**
     * 文件夹名称
     */
    @Schema(description = "文件夹名称")
    private String folderName;
    /**
     * 上级文件夹ID
     */
    @Schema(description = "上级文件夹ID，根目录可为空")
    private Long parentId;
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long accountId;
}
