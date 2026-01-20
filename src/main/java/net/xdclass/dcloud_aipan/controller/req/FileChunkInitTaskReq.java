package net.xdclass.dcloud_aipan.controller.req;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileChunkInitTaskReq {

    private Long accountId;

    private String filename;

    private String identifier;

    private Long totalSize;

    private Long chunkSize;

}
