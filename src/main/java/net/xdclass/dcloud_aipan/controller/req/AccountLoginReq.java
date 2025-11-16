package net.xdclass.dcloud_aipan.controller.req;

import lombok.Data;

@Data
public class AccountLoginReq {
    /**
     * 密码
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;
}
