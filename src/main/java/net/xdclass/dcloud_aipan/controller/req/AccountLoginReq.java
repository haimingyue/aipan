package net.xdclass.dcloud_aipan.controller.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "AccountLoginReq", description = "账号登录请求参数")
public class AccountLoginReq {
    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
}
