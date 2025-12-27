package net.xdclass.dcloud_aipan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.controller.req.FolderCreateReq;
import net.xdclass.dcloud_aipan.dto.AccountFileDTO;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.enums.FolderFlagEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.mapper.AccountFileMapper;
import net.xdclass.dcloud_aipan.model.AccountFileDO;
import net.xdclass.dcloud_aipan.service.AccountFileService;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class AccountFileServiceImpl implements AccountFileService {

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

    @Override
    public Long createFolder(FolderCreateReq req) {
        AccountFileDTO accountFileDTO = AccountFileDTO.builder()
                .accountId(req.getAccountId())
                .parentId(req.getParentId())
                .fileName(req.getFolderName())
                .isDir(FolderFlagEnum.YES.getCode())
                .build();
        return saveAccountFile(accountFileDTO);
    }

    private Long saveAccountFile(AccountFileDTO accountFileDTO) {
        // 检查父文件是否存在
        checkAccountFileId(accountFileDTO);
        // 检查文件是否重复 aa aa(1) aa(2)
        AccountFileDO accountFileDO = SpringBeanUtil.copyProperties(accountFileDTO, AccountFileDO.class);
        processAccountFileDuplicate(accountFileDO);
        // 保存相关文件关系
        accountFileMapper.insert(accountFileDO);
        return accountFileDO.getId();
    }

    /**
     * 文件名是否存在
     */
    private void processAccountFileDuplicate(AccountFileDO accountFileDO) {
        Long selectCount = accountFileMapper.selectCount(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountFileDO.getAccountId())
                .eq("parent_id", accountFileDO.getParentId())
                .eq("is_dir", accountFileDO.getIsDir())
                .eq("file_name", accountFileDO.getFileName()));
        if (selectCount > 0) {
            // 处理文件夹
            if (Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
                accountFileDO.setFileName(accountFileDO.getFileName() + "(1)");
            } else {
                // 处理文件：后续可按需求添加文件重名处理策略
                // 提前文件拓展名
                String[] split = accountFileDO.getFileName().split("\\.");
                String fileName = split[0];
                String fileSuffix = split[1];
                accountFileDO.setFileName(fileName + "(1)" + "." + fileSuffix);
            }
        }
    }

    private void checkAccountFileId(AccountFileDTO accountFileDTO) {
        Long parentId = accountFileDTO.getParentId();
        if (parentId != null) {
            AccountFileDO parentFile = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                    .eq("id", parentId)
                    .eq("account_id", accountFileDTO.getAccountId()));
            if (parentFile == null) {
                throw new BizException(BizCodeEnum.FILE_NOT_EXISTS );
            }
        }
    }
}
