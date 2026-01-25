package net.xdclass.dcloud_aipan.service;

import net.xdclass.dcloud_aipan.controller.req.ShareCancelReq;
import net.xdclass.dcloud_aipan.controller.req.ShareCheckReq;
import net.xdclass.dcloud_aipan.controller.req.ShareCreateReq;
import net.xdclass.dcloud_aipan.dto.ShareDTO;
import net.xdclass.dcloud_aipan.dto.ShareSimpleDTO;

import java.util.List;

public interface ShareService {
    List<ShareDTO> listShare();

    ShareDTO createShare(ShareCreateReq req);

    void cancelShare(ShareCancelReq req);

    ShareSimpleDTO simpleDetail(Long shareId);

    String checkShareCode(ShareCheckReq shareCheckReq);
}
