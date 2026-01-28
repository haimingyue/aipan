package net.xdclass.dcloud_aipan.controller.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShareFileTransferReq {

    private Long shareId;

    private Long accountId;

    private Long parentId;

    private List<Long> fileIds;

}
