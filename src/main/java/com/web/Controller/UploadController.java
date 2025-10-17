package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.StorageService;
import com.web.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器
 * 处理文件上传、头像上传等功能
 */
@RestController
@RequestMapping("/api/upload")
@Slf4j
public class UploadController {

    // 允许的文件扩展名白名单
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
        // 图片文件
        "jpg", "jpeg", "png", "gif", "bmp", "webp",
        // 文档文件
        "pdf", "doc", "docx", "txt", "rtf",
        // 压缩文件
        "zip", "rar", "7z", "tar", "gz"
    );

    // 文件MIME类型映射
    private static final Map<String, String> MIME_TYPE_MAP = Map.of(
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "png", "image/png",
        "gif", "image/gif",
        "bmp", "image/bmp",
        "webp", "image/webp",
        "pdf", "application/pdf",
        "doc", "application/msword",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "txt", "text/plain",
        "rtf", "application/rtf",
        "zip", "application/zip",
        "rar", "application/x-rar-compressed",
        "7z", "application/x-7z-compressed"
    );

    // 文件魔术字节（文件头）
    private static final Map<String, byte[]> FILE_SIGNATURES = Map.of(
        "jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
        "jpeg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
        "png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
        "gif", new byte[]{0x47, 0x49, 0x46, 0x38},
        "bmp", new byte[]{0x42, 0x4D},
        "pdf", new byte[]{0x25, 0x50, 0x44, 0x46},
        "zip", new byte[]{0x50, 0x4B, 0x03, 0x04},
        "rar", new byte[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00}
    );

    @Autowired
    private StorageService storageService;

    /**
     * 单文件上传
     */
    @PostMapping("/file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "files") String directory,
            @Userid Long userId) {

        log.info("用户 {} 上传文件: {}", userId, file.getOriginalFilename());

        // 验证文件
        validateFile(file);

        // 上传文件
        String fileUrl = storageService.uploadFile(file, directory);

        Map<String, Object> result = new HashMap<>();
        result.put("url", fileUrl);
        result.put("filename", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("contentType", file.getContentType());

        log.info("文件上传成功: {}", fileUrl);
        return ResponseEntity.ok(ApiResponse.success("文件上传成功", result));
    }

    /**
     * 多文件上传
     */
    @PostMapping("/files")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "directory", defaultValue = "files") String directory,
            @Userid Long userId) {

        log.info("用户 {} 批量上传 {} 个文件", userId, files.length);

        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile file : files) {
            // 验证文件
            validateFile(file);

            // 上传文件
            String fileUrl = storageService.uploadFile(file, directory);

            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("contentType", file.getContentType());
            results.add(result);
        }

        log.info("批量文件上传成功，共 {} 个文件", results.size());
        return ResponseEntity.ok(ApiResponse.success("批量上传成功", results));
    }

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            @Userid Long userId) {

        log.info("用户 {} 上传头像", userId);

        // 验证头像文件
        validateAvatarFile(avatar);

        // 上传到头像目录
        String avatarUrl = storageService.uploadFile(avatar, "avatars");

        Map<String, Object> result = new HashMap<>();
        result.put("avatarUrl", avatarUrl);
        result.put("filename", avatar.getOriginalFilename());
        result.put("size", avatar.getSize());

        log.info("头像上传成功: {}", avatarUrl);
        return ResponseEntity.ok(ApiResponse.success("头像上传成功", result));
    }

    /**
     * 群组头像上传
     */
    @PostMapping("/group-avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadGroupAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            @RequestParam("groupId") Long groupId,
            @Userid Long userId) {

        log.info("用户 {} 为群组 {} 上传头像", userId, groupId);

        // 验证头像文件
        validateAvatarFile(avatar);

        // 上传到群组头像目录
        String avatarUrl = storageService.uploadFile(avatar, "group-avatars/" + groupId);

        Map<String, Object> result = new HashMap<>();
        result.put("avatarUrl", avatarUrl);
        result.put("groupId", groupId);
        result.put("filename", avatar.getOriginalFilename());
        result.put("size", avatar.getSize());

        log.info("群组头像上传成功: {}", avatarUrl);
        return ResponseEntity.ok(ApiResponse.success("群组头像上传成功", result));
    }

    /**
     * 安全验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 文件大小限制（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 检查文件名安全性（防止路径遍历攻击）
        if (!ValidationUtils.validateFileName(filename)) {
            throw new IllegalArgumentException("文件名包含非法字符");
        }

        // 获取文件扩展名
        String extension = getFileExtension(filename).toLowerCase();

        // 白名单验证
        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }

        // MIME类型验证
        String expectedMimeType = MIME_TYPE_MAP.get(extension);
        if (expectedMimeType != null) {
            String actualMimeType = file.getContentType();
            if (actualMimeType == null || !actualMimeType.equals(expectedMimeType)) {
                throw new IllegalArgumentException("文件类型与扩展名不匹配");
            }
        }

        // 魔术字节验证（文件内容验证）
        if (!validateFileContent(file, extension)) {
            throw new IllegalArgumentException("文件内容验证失败，可能为恶意文件");
        }
    }

    /**
     * 验证文件内容（魔术字节）
     */
    private boolean validateFileContent(MultipartFile file, String extension) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[10];
            int bytesRead = inputStream.read(header);

            if (bytesRead <= 0) {
                return false;
            }

            byte[] expectedSignature = FILE_SIGNATURES.get(extension);
            if (expectedSignature != null) {
                return startsWith(header, expectedSignature);
            }

            // 对于没有定义魔术字节的文件类型，进行基本的检查
            return !isBinaryFile(header);
        } catch (IOException e) {
            log.error("验证文件内容失败", e);
            return false;
        }
    }

    /**
     * 检查字节数组是否以指定前缀开头
     */
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否为二进制文件
     */
    private boolean isBinaryFile(byte[] header) {
        // 检查前100字节中是否包含null字节
        int checkLength = Math.min(header.length, 100);
        for (int i = 0; i < checkLength; i++) {
            if (header[i] == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证头像文件
     */
    private void validateAvatarFile(MultipartFile avatar) {
        validateFile(avatar);

        // 检查是否为图片文件
        String contentType = avatar.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("头像必须是图片文件");
        }

        if (avatar.getSize() > 2 * 1024 * 1024) { // 2MB
            throw new IllegalArgumentException("头像文件大小不能超过2MB");
        }

        String extension = getFileExtension(avatar.getOriginalFilename());
        if (!isImageExtension(extension)) {
            throw new IllegalArgumentException("头像文件格式不支持: " + extension);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * 检查是否为危险文件扩展名
     */
    private boolean isDangerousExtension(String extension) {
        String[] dangerousExtensions = {
            "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar", "php", "asp", "jsp"
        };
        for (String dangerous : dangerousExtensions) {
            if (dangerous.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为图片文件扩展名
     */
    private boolean isImageExtension(String extension) {
        String[] imageExtensions = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
        };
        for (String imgExt : imageExtensions) {
            if (imgExt.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}

