package net.xdclass.dcloud_aipan.dto;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 用户信息表
 * </p>
 *
 * @author SimoonQian,
 * @since 2025-11-03
 */
@Getter
@Setter
@Schema(name = "AccountDTO", description = "用户信息表")
public class AccountDTO implements Serializable {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户头像")
    private String avatarUrl;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "用户角色 COMMON, ADMIN")
    private String role;

    @Schema(description = "逻辑删除（1删除 0未删除）")
    @TableLogic
    private Boolean del;

    @Schema(description = "创建时间")
    private Date gmtCreate;

    @Schema(description = "更新时间")
    private Date gmtModified;
}
