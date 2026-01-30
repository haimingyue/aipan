package net.xdclass.dcloud_aipan.mapper;

import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.model.AccountFileDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户文件关联表 Mapper 接口
 * </p>
 *
 * @author SimoonQian,
 * @since 2025-11-03
 */
public interface AccountFileMapper extends BaseMapper<AccountFileDO> {

    void insertFileBatch(@Param("newAccountFileDOList") List<AccountFileDO> newAccountFileDOList);

    /**
     * 查询被删除的文件
     */
    List<AccountFileDO> selectRecycleFiles(@Param("accountId") Long accountId, @Param("fileIdList") List<Long> fileIdList);
}
