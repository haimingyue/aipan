package net.xdclass.dcloud_aipan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Schema(name = "ShareSimpleDTO", description = "分享链接简单对象")
@Accessors(chain = true)
public class ShareSimpleDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "分享id")
    private Long id;

    @Schema(description = "分享名称")
    private String shareName;

    @Schema(description = "分享类型（no_code没有提取码 ,need_code有提取码）")
    private String shareType;

    @Schema(description = "分享类型（0 永久有效；1: 7天有效；2: 30天有效）")
    private Integer shareDayType;

    @Schema(description = "分享有效天数（永久有效为0）")
    private Integer shareDay;

    @Schema(description = "分享结束时间")
    private Date shareEndTime;

    @Schema(description = "分享链接地址")
    private String shareUrl;

    /**
     * 不需要code 的时候使用
     */
    private String shareToken;

    private ShareAccountDTO shareAccountDTO;

}
