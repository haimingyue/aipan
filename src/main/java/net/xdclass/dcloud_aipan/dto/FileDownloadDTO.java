package net.xdclass.dcloud_aipan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileDownloadDTO {

    private String fileName;

    private String downloadUrl;
}
