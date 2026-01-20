package net.xdclass.dcloud_aipan.dto;

import com.amazonaws.services.s3.model.PartSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.xdclass.dcloud_aipan.model.FileChunkDO;
import net.xdclass.dcloud_aipan.util.SpringBeanUtil;

import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FileChunkDTO {

    public FileChunkDTO(FileChunkDO fileChunkDO) {
        SpringBeanUtil.copyProperties(fileChunkDO, this);
    }

    private Long id;

    @Schema(description = "文件唯一标识（md5）")
    private String identifier;

    @Schema(description = "分片上传ID")
    private String uploadId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "所属桶名")
    private String bucketName;

    @Schema(description = "文件的key")
    private String objectKey;

    @Schema(description = "总文件大小（byte）")
    private Long totalSize;

    @Schema(description = "每个分片大小（byte）")
    private Long chunkSize;

    @Schema(description = "分片数量")
    private Integer chunkNum;

    @Schema(description = "用户ID")
    private Long accountId;

    private boolean finished;

    /**
     * 返回已经存在的分片
     */
    private List<PartSummary> existPartList;
}
