package net.xdclass.dcloud_aipan.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 存储信息表
 * </p>
 *
 * @author SimoonQian,
 * @since 2025-11-03
 */
@Getter
@Setter
@Schema(name = "StorageDO", description = "存储信息表")
public class StorageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "所属用户")
    private Long accountId;

    @Schema(description = "占用存储大小")
    private Long usedSize;

    @Schema(description = "总容量大小，字节存储")
    private Long totalSize;

}
