package com.web.service;

import com.web.exception.WeebException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传安全验证服务
 * 实现文件类型、大小、内容验证
 */
@Slf4j
@Service
public class FileUploadSecurityService {

    // 允许的文件类型白名单
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
        "application/pdf", 
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    // 文件大小限制（字节）
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    // Magic bytes for file type validation
    private static final Map<String, byte[]> MAGIC_BYTES = new HashMap<>();
    
    static {
        // JPEG
        MAGIC_BYTES.put("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        // PNG
        MAGIC_BYTES.put("image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        // GIF
        MAGIC_BYTES.put("image/gif", new byte[]{0x47, 0x49, 0x46, 0x38});
        // WebP
        MAGIC_BYTES.put("image/webp", new byte[]{0x52, 0x49, 0x46, 0x46});
        // PDF
        MAGIC_BYTES.put("application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
    }

    /**
     * 验证上传的文件
     * @param file 上传的文件
     * @param fileType 文件类型（image, document, general）
     * @return 验证结果
     */
    public ValidationResult validateFile(MultipartFile file, String fileType) {
        ValidationResult result = new ValidationResult();
        
        try {
            // 1. 检查文件是否为空
            if (file == null || file.isEmpty()) {
                result.setValid(false);
                result.setMessage("文件不能为空");
                return result;
            }

            // 2. 检查文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                result.setValid(false);
                result.setMessage("文件名不能为空");
                return result;
            }

            // 3. 检查文件大小
            long fileSize = file.getSize();
            if (!validateFileSize(fileSize, fileType)) {
                result.setValid(false);
                result.setMessage("文件大小超过限制");
                return result;
            }

            // 4. 检查MIME类型
            String contentType = file.getContentType();
            if (!validateMimeType(contentType, fileType)) {
                result.setValid(false);
                result.setMessage("不支持的文件类型: " + contentType);
                return result;
            }

            // 5. 验证文件内容（Magic Bytes）
            if (!validateFileContent(file, contentType)) {
                result.setValid(false);
                result.setMessage("文件内容与声明的类型不匹配");
                return result;
            }

            // 6. 检查文件扩展名
            String extension = getFileExtension(originalFilename);
            if (!validateFileExtension(extension, contentType)) {
                result.setValid(false);
                result.setMessage("文件扩展名与类型不匹配");
                return result;
            }

            result.setValid(true);
            result.setMessage("文件验证通过");
            result.setFileSize(fileSize);
            result.setContentType(contentType);
            result.setExtension(extension);

        } catch (Exception e) {
            log.error("文件验证失败", e);
            result.setValid(false);
            result.setMessage("文件验证过程中发生错误: " + e.getMessage());
        }

        return result;
    }

    /**
     * 验证文件大小
     */
    private boolean validateFileSize(long fileSize, String fileType) {
        switch (fileType.toLowerCase()) {
            case "image":
                return fileSize <= MAX_IMAGE_SIZE;
            case "document":
                return fileSize <= MAX_DOCUMENT_SIZE;
            default:
                return fileSize <= MAX_FILE_SIZE;
        }
    }

    /**
     * 验证MIME类型
     */
    private boolean validateMimeType(String contentType, String fileType) {
        if (contentType == null) {
            return false;
        }

        switch (fileType.toLowerCase()) {
            case "image":
                return ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
            case "document":
                return ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase());
            default:
                return ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase()) ||
                       ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase());
        }
    }

    /**
     * 验证文件内容（Magic Bytes）
     */
    private boolean validateFileContent(MultipartFile file, String contentType) {
        try (InputStream is = file.getInputStream()) {
            byte[] magicBytes = MAGIC_BYTES.get(contentType);
            if (magicBytes == null) {
                // 如果没有定义magic bytes，跳过此检查
                return true;
            }

            byte[] fileHeader = new byte[magicBytes.length];
            int bytesRead = is.read(fileHeader);
            
            if (bytesRead < magicBytes.length) {
                return false;
            }

            return Arrays.equals(fileHeader, magicBytes);

        } catch (IOException e) {
            log.error("读取文件内容失败", e);
            return false;
        }
    }

    /**
     * 验证文件扩展名
     */
    private boolean validateFileExtension(String extension, String contentType) {
        if (extension == null || contentType == null) {
            return false;
        }

        Map<String, List<String>> extensionMap = new HashMap<>();
        extensionMap.put("image/jpeg", Arrays.asList("jpg", "jpeg"));
        extensionMap.put("image/png", Arrays.asList("png"));
        extensionMap.put("image/gif", Arrays.asList("gif"));
        extensionMap.put("image/webp", Arrays.asList("webp"));
        extensionMap.put("application/pdf", Arrays.asList("pdf"));

        List<String> validExtensions = extensionMap.get(contentType.toLowerCase());
        if (validExtensions == null) {
            return true; // 未定义的类型，跳过检查
        }

        return validExtensions.contains(extension.toLowerCase());
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 生成安全的文件名
     */
    public String generateSafeFilename(String originalFilename) {
        if (originalFilename == null) {
            return java.util.UUID.randomUUID().toString();
        }

        String extension = getFileExtension(originalFilename);
        String uuid = java.util.UUID.randomUUID().toString();
        
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private long fileSize;
        private String contentType;
        private String extension;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }
    }
}
