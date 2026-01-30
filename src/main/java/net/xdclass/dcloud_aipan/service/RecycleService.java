package net.xdclass.dcloud_aipan.service;

import net.xdclass.dcloud_aipan.dto.AccountFileDTO;

import java.util.List;

public interface RecycleService {
    List<AccountFileDTO> listRecycleFile(Long accountId);
}
