package com.web.vo.file;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 文件上传响应VO
 */
@Data
public class FileUploadVo {
    @NotNull
    private Long fileId;
    
    @NotNull
    private String fileName;
    
    @NotNull
    private String fileUrl;
    
    @NotNull
    private Long fileSize;
    
    @NotNull
    private String mimeType;
}