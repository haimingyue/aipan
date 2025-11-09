package net.xdclass.dcloud_aipan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.component.MinIoFileStoreEngine;
import net.xdclass.dcloud_aipan.config.AccountConfig;
import net.xdclass.dcloud_aipan.config.MinioConfig;
import net.xdclass.dcloud_aipan.controller.req.AccountRegisterReq;
import net.xdclass.dcloud_aipan.enums.AccountRoleEnum;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.mapper.AccountMapper;
import net.xdclass.dcloud_aipan.model.AccountDO;
import net.xdclass.dcloud_aipan.service.AccountService;
import net.xdclass.dcloud_aipan.util.CommonUtil;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private MinIoFileStoreEngine minIoFileStoreEngine;
    @Autowired
    private MinioConfig minioConfig;


    /**
     *
     * 3. 插入数据库
     * 4. 其他相关初始化操作
     * @param req
     */
    @Override
    public void register(AccountRegisterReq req) {
        // 1. 查询手机号是否注册
        List<AccountDO> accountDOList =  accountMapper.selectList(new QueryWrapper<AccountDO>().eq("phone", req.getPhone()));
        if(!accountDOList.isEmpty()){
            throw new BizException(BizCodeEnum.ACCOUNT_REPEAT);
        }

        AccountDO accountDO = SpringBeanUtil.copyProperties(req, AccountDO.class);
        // 2. 加密密码
        String digestAsHex = DigestUtils.md5DigestAsHex((AccountConfig.ACCOUNT_SALT + req.getPassword()).getBytes());
        // 3. 插入数据库
        accountDO.setPassword(digestAsHex);
        accountDO.setRole(AccountRoleEnum.COMMON.name());
        accountMapper.insert(accountDO);
        // 4. 其他操作 TODO
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        String filename = CommonUtil.getFilePath(file.getOriginalFilename());
        minIoFileStoreEngine.upload(minioConfig.getAvatarBucketName(), filename, file);
        return minioConfig.getEndpoint() + "/" + minioConfig.getAvatarBucketName() + "/" + filename;
    }
}
