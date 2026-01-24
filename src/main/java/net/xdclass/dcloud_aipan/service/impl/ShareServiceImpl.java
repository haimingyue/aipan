package net.xdclass.dcloud_aipan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.dto.AccountDTO;
import net.xdclass.dcloud_aipan.dto.ShareDTO;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.mapper.ShareFileMapper;
import net.xdclass.dcloud_aipan.mapper.ShareMapper;
import net.xdclass.dcloud_aipan.model.ShareDO;
import net.xdclass.dcloud_aipan.service.ShareService;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ShareServiceImpl implements ShareService {

    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private ShareFileMapper shareFileMapper;

    @Override
    public List<ShareDTO> listShare() {
        AccountDTO accountDTO = LoginInterceptor.threadLocal.get();

        List<ShareDO> shareDOList = shareMapper.selectList(new QueryWrapper<ShareDO>()
                .eq("account_id", accountDTO.getId())
                .orderByDesc("gmt_create"));
         return SpringBeanUtil.copyProperties(shareDOList, ShareDTO.class);
    }
}
