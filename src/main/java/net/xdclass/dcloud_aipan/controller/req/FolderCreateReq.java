package net.xdclass.dcloud_aipan.controller.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FolderCreateReq {
    /**
     * 文件夹名称
     */
    private String folderName;
    /**
     * 上级文件夹ID
     */
    private Long parentId;
    /**
     * 用户ID
     */
    private Long accountId;
}
