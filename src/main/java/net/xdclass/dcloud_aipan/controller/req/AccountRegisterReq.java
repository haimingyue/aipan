package net.xdclass.dcloud_aipan.controller.req;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountRegisterReq {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatarUrl;

}
