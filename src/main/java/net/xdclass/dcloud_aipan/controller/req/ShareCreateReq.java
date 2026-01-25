package net.xdclass.dcloud_aipan.controller.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.xdclass.dcloud_aipan.dto.ShareDTO;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.service.ShareService;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.web.bind.annotation.RequestBody;

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
