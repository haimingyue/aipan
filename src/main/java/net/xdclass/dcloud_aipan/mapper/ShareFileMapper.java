package net.xdclass.dcloud_aipan.mapper;

import net.xdclass.dcloud_aipan.model.ShareFileDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * 文件分享表 Mapper 接口
 * </p>
 *
 * @author SimoonQian,
 * @since 2025-11-03
 */
public interface ShareFileMapper extends BaseMapper<ShareFileDO> {

    void insertBatch(List<ShareFileDO> shareFileDOList);
}
