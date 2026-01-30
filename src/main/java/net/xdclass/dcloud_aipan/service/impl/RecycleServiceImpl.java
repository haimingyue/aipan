package net.xdclass.dcloud_aipan.service.impl;


import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.mapper.AccountFileMapper;
import net.xdclass.dcloud_aipan.model.AccountFileDO;
import net.xdclass.dcloud_aipan.service.RecycleService;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecycleServiceImpl implements RecycleService  {

    @Autowired
    private AccountFileMapper accountFileMapper;

    @Override
    public List<AccountFileDTO> listRecycleFile(Long accountId) {

        List<AccountFileDO> recycleList = accountFileMapper.selectRecycleFiles(accountId,  null);

        // 如果是文件夹，就只显示文件夹，不显示文件
        List<Long> fileIds = recycleList.stream().map(AccountFileDO::getId).toList();

        // 需要提取全部删除文件的ID, 然后过滤下, 如果某个文件的父 ID在这个文件 ID 集合下面，则不显示
        List<AccountFileDO> accountFileDOS = recycleList.stream().filter(accountFileDO -> fileIds.contains(accountFileDO.getParentId()))
                .collect(Collectors.toList());

        return SpringBeanUtil.copyProperties(accountFileDOS, AccountFileDTO.class);
    }
}
