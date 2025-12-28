package net.xdclass.dcloud_aipan.controller.req;

import lombok.Data;

@Data
public class FileUpdateReq {
    private Long accountId;
    private Long fileId;
    private String newFileName;
}
