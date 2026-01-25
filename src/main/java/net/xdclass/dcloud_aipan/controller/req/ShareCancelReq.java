package net.xdclass.dcloud_aipan.controller.req;

import lombok.Data;

import java.util.List;

@Data
public class ShareCancelReq {

    private List<Long> shareIds;

    private Long accountId;
}
