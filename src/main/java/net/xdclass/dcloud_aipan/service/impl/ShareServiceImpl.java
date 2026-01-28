package net.xdclass.dcloud_aipan.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.config.AccountConfig;
import net.xdclass.dcloud_aipan.controller.req.*;
import net.xdclass.dcloud_aipan.dto.*;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.enums.ShareDayEnum;
import net.xdclass.dcloud_aipan.enums.ShareStatusEnum;
import net.xdclass.dcloud_aipan.enums.ShareTypeEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.interceptor.LoginInterceptor;
import net.xdclass.dcloud_aipan.mapper.AccountFileMapper;
import net.xdclass.dcloud_aipan.mapper.AccountMapper;
import net.xdclass.dcloud_aipan.mapper.ShareFileMapper;
import net.xdclass.dcloud_aipan.mapper.ShareMapper;
import net.xdclass.dcloud_aipan.model.AccountDO;
import net.xdclass.dcloud_aipan.model.AccountFileDO;
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
import java.util.stream.Collectors;

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

    @Autowired
    private AccountFileMapper accountFileMapper;


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
     * 1. 查询分享记录实体
     * 2. 检查分享状态
     * 3. 查询分享文件信息
     */
    @Override
    public ShareDetailDTO detail(Long shareId) {
        ShareDO shareDO = checkShareStatus(shareId);
        ShareDetailDTO shareDetailDTO = SpringBeanUtil.copyProperties(shareDO, ShareDetailDTO.class);

        List<AccountFileDO> accountFileDOList = getShareFileInfo(shareId);
        List<AccountFileDTO> accountFileDTOS = SpringBeanUtil.copyProperties(accountFileDOList, AccountFileDTO.class);
        shareDetailDTO.setFileDTOList(accountFileDTOS);

        // 查询分享者信息
        ShareAccountDTO shareAccountDTO = getShareAccount(shareDO.getAccountId());
        shareDetailDTO.setShareAccountDTO(shareAccountDTO);
        return shareDetailDTO;
    }

    @Override
    public List<AccountFileDTO> listShareFile(ShareFileQueryReq req) {

        ShareDO shareDO = checkShareStatus(req.getShareId());

        // 查询分享 id 是否在分享列表中
        List<AccountFileDO> accountFileDOList = checkShareFileIdOnStatus(shareDO.getId(),  List.of(req.getParentId()));

        List<AccountFileDTO> accountFileDTOList = SpringBeanUtil.copyProperties(accountFileDOList, AccountFileDTO.class);

        //分组后获取某个文件夹下面所有的子文件夹
        Map<Long, List<AccountFileDTO>> fileListMap = accountFileDTOList.stream()
                .collect(Collectors.groupingBy(AccountFileDTO::getParentId));

        //根据父文件夹ID获取子文件夹列表
        List<AccountFileDTO> childFileList = fileListMap.get(req.getParentId());

        if(CollectionUtils.isEmpty(childFileList)){
            return List.of();
        }

        return childFileList;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferShareFile(ShareFileTransferReq req) {
        //分享链接是否状态准确
        checkShareStatus(req.getShareId());

        //转存的文件是否是分享链接里面的文件
        checkInShareFiles(req.getFileIds(),req.getShareId());

        //目标文件夹是否是当前用户的
        AccountFileDO currentAccountDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("id", req.getParentId()).eq("account_id", req.getAccountId()));
        if(currentAccountDO == null){
            log.error("目标文件夹不是当前用户的,{}",req);
            throw new BizException(BizCodeEnum.FILE_NOT_EXISTS);
        }

        //获取转存的文件
        List<AccountFileDO> shareFileList = accountFileMapper.selectBatchIds(req.getFileIds());
        //保存需要转存的文件列表（递归子文件）
        List<AccountFileDO> batchTransferFileList = accountFileService.findBatchCopyWithRecur(shareFileList, req.getParentId());

        //同步更新所有文件的accountId为当前用户的id
        batchTransferFileList.forEach(file -> {
            file.setAccountId(req.getAccountId());
        });

        //计算存储空间大小，检查是否足够
        if(!accountFileService.checkAndUpdateCapacity(req.getAccountId(),batchTransferFileList.stream()
                .map(accountFileDO -> accountFileDO.getFileSize() == null ? 0 : accountFileDO.getFileSize())
                .mapToLong(Long::valueOf).sum())){
            throw new BizException(BizCodeEnum.FILE_STORAGE_NOT_ENOUGH);
        }
        //更新关联对象信息，存储文件映射关系
        accountFileMapper.insertFileBatch(batchTransferFileList);
    }

    private void checkInShareFiles(List<Long> fileIds, Long shareId) {
        //获取分享链接的文件
        List<ShareFileDO> shareFileDOS = shareFileMapper.selectList(new QueryWrapper<ShareFileDO>().eq("share_id", shareId));
        List<Long> shareFileIds = shareFileDOS.stream().map(ShareFileDO::getAccountFileId).toList();
        //找文件实体
        List<AccountFileDO> shareAccountFileDOList = accountFileMapper.selectBatchIds(shareFileIds);
        //递归找分享链接里面的所有子文件
        List<AccountFileDO> allShareFiles = new ArrayList<>();
        accountFileService.findAllAccountFileDOWithRecur(allShareFiles, shareAccountFileDOList, false);
        //提取全部文件的ID
        List<Long> allShareFileIds = allShareFiles.stream().map(AccountFileDO::getId).toList();

        //判断要转存的文件是否在里面
        for (Long fileId : fileIds) {
            if(!allShareFileIds.contains(fileId)){
                log.error("文件不在分享链接里面，fileId:{}",fileId);
                throw new BizException(BizCodeEnum.SHARE_FILE_ILLEGAL);
            }
        }
    }

    private List<AccountFileDO> checkShareFileIdOnStatus(Long shareId, List<Long> fileIdList) {
        //需要获取分享文件列表的全部文件夹和子文件内容
        List<AccountFileDO>  shareFileInfoList = getShareFileInfo(shareId);
        List<AccountFileDO> allAccountFileDOList = new ArrayList<>();
        //获取全部文件，递归
        accountFileService.findAllAccountFileDOWithRecur(allAccountFileDOList, shareFileInfoList, false);

        if(CollectionUtils.isEmpty(allAccountFileDOList)){
            return List.of();
        }

        //把分享的对象文件的全部文件夹放到集合里面，判断目标文件集合是否都在里面
        Set<Long> allFileIdSet = allAccountFileDOList.stream().map(AccountFileDO::getId).collect(Collectors.toSet());
        if(!allFileIdSet.containsAll(fileIdList)){
            log.error("目标文件ID列表 不再 分享的文件列表中,{}",fileIdList);
            throw new BizException(BizCodeEnum.SHARE_FILE_ILLEGAL);
        }
        return allAccountFileDOList;
    }


    private List<AccountFileDO> getShareFileInfo(Long shareId) {
        // 查找分享文件列表
//        List<Long> shareFileIdList = getShareFileIdList(shareId);
        List<ShareFileDO> shareFileDOS = shareFileMapper.selectList(new QueryWrapper<ShareFileDO>().select("account_file_id").eq("share_id", shareId));
        List<Long> shareFileIdList = shareFileDOS.stream().map(ShareFileDO::getAccountFileId).toList();

        return accountFileMapper.selectBatchIds(shareFileIdList);
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
//        if (ShareStatusEnum.EXPIRED.name().equalsIgnoreCase(shareDO.getShareStatus())) {
//            log.error("分享已失效");
//            throw new BizException(BizCodeEnum.SHARE_EXPIRED);
//        }
        // 暂时未用，直接物理删除，可以调整
        if (ShareStatusEnum.CANCELED.name().equalsIgnoreCase(shareDO.getShareStatus())) {
            log.error("分享已取消");
            throw new BizException(BizCodeEnum.SHARE_CANCELED);
        }
        // 判断分享是否过期
        if (shareDO.getShareEndTime().getTime() < System.currentTimeMillis()) {
            log.error("分享已失效");
            throw new BizException(BizCodeEnum.SHARE_EXPIRED);
        }
        return shareDO;
    }
}
