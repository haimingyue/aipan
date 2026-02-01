package net.xdclass.dcloud_aipan.controller.req;

import lombok.Data;

import java.util.List;

@Data
public class RecycleRestoreReq {

    private List<Long> fileIds;

    private Long accountId;
}
