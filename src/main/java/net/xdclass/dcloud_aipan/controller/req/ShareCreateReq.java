package net.xdclass.dcloud_aipan.controller.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShareCreateReq {

    private String shareName;

    private String shareType;

    // 0 永久 1 七天 2 30天
    private Integer shareDayType;

    private List<Long> fileIds;

    private Long AccountId;
}
