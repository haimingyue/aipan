package net.xdclass.dcloud_aipan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderTreeNodeDTO {
    private Long id;
    private Long parentId;
    private String label;

    private List<FolderTreeNodeDTO> children = new ArrayList<>();
}
