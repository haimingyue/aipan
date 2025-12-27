package net.xdclass.dcloud_aipan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FolderFlagEnum {

    /**
     * 是否是文件夹
     * 0: 不是文件夹
     * 1: 是文件夹
     */
    NO(0)
    , YES(1);
    private final Integer code;
}
