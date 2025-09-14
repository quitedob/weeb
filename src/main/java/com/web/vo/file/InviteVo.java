package com.web.vo.file;

import lombok.Data;

@Data
public class InviteVo {
    private Long TargetId;
    private Long userId;
    private FileInfo fileInfo;

    @Data
    public static class FileInfo {
        private String name;
        private long size;
    }
}
