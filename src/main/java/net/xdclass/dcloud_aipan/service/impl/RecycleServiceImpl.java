package net.xdclass.dcloud_aipan.service.impl;


import net.xdclass.dcloud_aipan.controller.req.RecycleDeleteReq;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.enums.FolderFlagEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.mapper.AccountFileMapper;
import net.xdclass.dcloud_aipan.model.AccountFileDO;
import net.xdclass.dcloud_aipan.service.AccountService;
import net.xdclass.dcloud_aipan.service.RecycleService;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecycleServiceImpl implements RecycleService  {

    @Autowired
    private AccountFileMapper accountFileMapper;

    @Autowired
    private AccountService accountService;

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

    /**
     * 彻底删除回收站
     * 1. 文件ID数量是否合法
     * 2. 判断文件是否是文件夹
     * 3. 批量删除回收站文件
     */
    @Override
    public void delete(RecycleDeleteReq req) {

        List<AccountFileDO> records = accountFileMapper.selectRecycleFiles(req.getAccountId(), req.getFileIds());

        if (records.size() != req.getFileIds().size()) {
            throw new BizException(BizCodeEnum.FILE_DEL_BATCH_ILLEGAL);
        }

        // 判断是不是文件夹
        List<AccountFileDO> allRecords = new ArrayList<>();

        // 需要单独写一个方法，查询文件夹和子文件夹的递归方法，需要del = 1
        findAllAccountFileDOWithRecur(allRecords, records, false);

        List<Long> recycleFileIds = allRecords.stream().map(AccountFileDO::getId).toList();

        //批量删除回收站文件
        accountFileMapper.deleteRecycleFiles(recycleFileIds);


    }

    private void findAllAccountFileDOWithRecur(List<AccountFileDO> allRecords, List<AccountFileDO> records, boolean onlyFolder) {
        for(AccountFileDO accountFileDO : records){
            if(Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())){
                //递归查找 del=1
                List<AccountFileDO> childAccountFileDOList = accountFileMapper.selectRecycleChildFiles(accountFileDO.getId(),accountFileDO.getAccountId());
                findAllAccountFileDOWithRecur(allRecords,childAccountFileDOList,onlyFolder);
            }

            //如果通过onlyFolder是true,只存储文件夹到allAccountFileDOList，否则都存储到allAccountFileDOList
            if(!onlyFolder || Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())){
                allRecords.add(accountFileDO);
            }
        }
    }
}
