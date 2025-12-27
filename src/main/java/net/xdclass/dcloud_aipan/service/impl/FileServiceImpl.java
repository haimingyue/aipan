package net.xdclass.dcloud_aipan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.mapper.AccountFileMapper;
import net.xdclass.dcloud_aipan.model.AccountFileDO;
import net.xdclass.dcloud_aipan.service.FileService;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private AccountFileMapper accountFileMapper;

    /**
     * 获取文件列表接口
     */
    @Override
    public List<AccountFileDTO> listFile(Long accountId, Long parentId) {
        List<AccountFileDO> accountFileDOList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .eq("parent_id", parentId)
                .orderByDesc("is_dir")
                .orderByDesc("gmt_create"));
        return SpringBeanUtil.copyProperties(accountFileDOList, AccountFileDTO.class);
    }
}
