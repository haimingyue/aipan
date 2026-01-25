package net.xdclass.dcloud_aipan.service;

import net.xdclass.dcloud_aipan.controller.req.ShareCreateReq;
import net.xdclass.dcloud_aipan.dto.ShareDTO;

import java.util.List;

public interface ShareService {
    List<ShareDTO> listShare();

    ShareDTO createShare(ShareCreateReq req);
}
