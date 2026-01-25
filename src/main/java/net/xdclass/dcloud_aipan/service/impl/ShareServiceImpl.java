package net.xdclass.dcloud_aipan.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.config.AccountConfig;
import net.xdclass.dcloud_aipan.controller.req.ShareCancelReq;
import net.xdclass.dcloud_aipan.controller.req.ShareCheckReq;
import net.xdclass.dcloud_aipan.controller.req.ShareCreateReq;
import net.xdclass.dcloud_aipan.dto.AccountDTO;
import net.xdclass.dcloud_aipan.dto.ShareAccountDTO;
import net.xdclass.dcloud_aipan.dto.ShareDTO;
import net.xdclass.dcloud_aipan.dto.ShareSimpleDTO;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.enums.ShareDayEnum;
import net.xdclass.dcloud_aipan.enums.ShareStatusEnum;
import net.xdclass.dcloud_aipan.enums.ShareTypeEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.mapper.AccountMapper;
import net.xdclass.dcloud_aipan.mapper.ShareFileMapper;
import net.xdclass.dcloud_aipan.mapper.ShareMapper;
import net.xdclass.dcloud_aipan.model.AccountDO;
import net.xdclass.dcloud_aipan.model.ShareDO;
import net.xdclass.dcloud_aipan.model.ShareFileDO;
import net.xdclass.dcloud_aipan.service.AccountFileService;
import net.xdclass.dcloud_aipan.service.ShareService;
import net.xdclass.dcloud_aipan.util.JwtUtil;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
public class ShareServiceImpl implements ShareService {


    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private ShareFileMapper shareFileMapper;

    @Autowired
    private AccountFileService accountFileService;

    @Autowired
    private AccountMapper accountMapper;

    @Override
    public List<ShareDTO> listShare() {
        AccountDTO accountDTO = LoginInterceptor.threadLocal.get();

        List<ShareDO> shareDOList = shareMapper.selectList(new QueryWrapper<ShareDO>()
                .eq("account_id", accountDTO.getId())
                .orderByDesc("gmt_create"));
         return SpringBeanUtil.copyProperties(shareDOList, ShareDTO.class);
    }

    /**
     * 检查分享文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareDTO createShare(ShareCreateReq req) {
        //1.
        List<Long> fileIds = req.getFileIds();
        accountFileService.checkFileIdLegal(fileIds, req.getAccountId());

        //2. 生成分享链接，持久化数据库
        Integer shareDayType = req.getShareDayType();
        Integer shareDays = ShareDayEnum.getDaysByType(shareDayType);

        Long shareId = IdUtil.getSnowflakeNextId();

        // 生成分享链接
        String shareUrl = AccountConfig.PAN_FRONT_DOMAIN_SHARE_API + shareId;
        log.info("shareUrl:{}", shareUrl);

        // 拼装分享对象
        ShareDO shareDO = ShareDO.builder().id(shareId)
                .shareName(req.getShareName())
                .shareType(ShareTypeEnum.valueOf(req.getShareType()).name())
                .shareDayType(shareDayType)
                .shareDay(shareDays)
                .shareUrl(shareUrl)
                .shareStatus(ShareStatusEnum.USED.name())
                .accountId(req.getAccountId())
                .build();

        if (ShareDayEnum.PERMENENT.getDayType().equals(shareDayType)) {
            shareDO.setShareEndTime(Date.from(LocalDate.of(9999, 12, 31)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        } else {
            shareDO.setShareEndTime(new Date(System.currentTimeMillis() + shareDays * 24 * 60 * 60 * 1000));
        }

        if (ShareTypeEnum.NEED_CODE.name().equalsIgnoreCase(req.getShareType())) {
            // 返回生成提取码 6 位
            String shareCode = RandomStringUtils.randomAlphabetic(6).toUpperCase();
            shareDO.setShareCode(shareCode);
        }
        shareMapper.insert(shareDO);

        //3. 详情持久化数据库
        List<ShareFileDO> shareFileDOList = new ArrayList<>();
        for (Long fileId : fileIds) {
            ShareFileDO shareFileDO = ShareFileDO.builder()
                    .shareId(shareId)
                    .accountFileId(fileId)
                    .accountId(req.getAccountId())
                    .build();
            shareFileDOList.add(shareFileDO);
        }

        shareFileMapper.insertBatch(shareFileDOList);

        return SpringBeanUtil.copyProperties(shareDO, ShareDTO.class);
    }

    @Override
    public void cancelShare(ShareCancelReq req) {
        List<ShareDO> shareDOList = shareMapper.selectList(new QueryWrapper<ShareDO>()
                .eq("account_id", req.getAccountId())
                .in("id", req.getShareIds()));

        if (shareDOList.size() != req.getShareIds().size()) {
            log.error("分享不存在");
            throw new BizException(BizCodeEnum.SHARE_NOT_EXIST);
        }
        // 删除分享链接
        shareMapper.deleteBatchIds(req.getShareIds());

        // 删除分享详情
        shareFileMapper.delete(new QueryWrapper<ShareFileDO>().in("share_id", req.getShareIds()));
    }

    @Override
    public ShareSimpleDTO simpleDetail(Long shareId) {
        // 查看分享状态
        ShareDO shareDO = checkShareStatus(shareId);
        ShareSimpleDTO shareSimpleDTO = SpringBeanUtil.copyProperties(shareDO, ShareSimpleDTO.class);

        // 查询分享者信息
        ShareAccountDTO shareAccountDTO = getShareAccount(shareDO.getAccountId());
        shareSimpleDTO.setShareAccountDTO(shareAccountDTO);

        // 判断是否需要校验码
        if (ShareTypeEnum.NEED_CODE.name().equalsIgnoreCase(shareDO.getShareType())) {
            String shareToken = JwtUtil.geneShareJWT(shareDO.getId());
            shareSimpleDTO.setShareToken(shareToken);
        }

        return shareSimpleDTO;
    }

    @Override
    public String checkShareCode(ShareCheckReq shareCheckReq) {

        ShareDO shareDO = shareMapper.selectOne(new QueryWrapper<ShareDO>()
                .eq("id", shareCheckReq.getShareId())
                .eq("share_code", shareCheckReq.getShareCode())
                .eq("share_status", ShareStatusEnum.USED.name()));

        if (shareDO != null) {
            // 判断是否过期
            if (shareDO.getShareEndTime().getTime() < System.currentTimeMillis()) {
                return JwtUtil.geneShareJWT(shareDO.getId());
            } else {
                log.error("分享已失效 {}", shareDO.getId());
                throw new BizException(BizCodeEnum.SHARE_EXPIRED);
            }
        }

        return "";
    }

    /**
     * 获取分享者信息
     */
    private ShareAccountDTO getShareAccount(Long accountId) {
        if (accountId != null) {
            AccountDO accountDO = accountMapper.selectById(accountId);
            if (accountDO != null) {
                return SpringBeanUtil.copyProperties(accountDO, ShareAccountDTO.class);
            }
        }
        return null;
    }

    private ShareDO checkShareStatus(Long shareId) {
        ShareDO shareDO = shareMapper.selectById(shareId);
        if (shareDO == null) {
            log.error("分享不存在");
            throw new BizException(BizCodeEnum.SHARE_NOT_EXIST);
        }
        if (ShareStatusEnum.EXPIRED.name().equalsIgnoreCase(shareDO.getShareStatus())) {
            log.error("分享已失效");
            throw new BizException(BizCodeEnum.SHARE_EXPIRED);
        }
        if (ShareStatusEnum.CANCELED.name().equalsIgnoreCase(shareDO.getShareStatus())) {
            log.error("分享已取消");
            throw new BizException(BizCodeEnum.SHARE_CANCELED);
        }
        return shareDO;
    }
}
