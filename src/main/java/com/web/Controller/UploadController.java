package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 * 处理文件上传、头像上传等功能
 */
@RestController
@RequestMapping("/api/upload")
@Slf4j
public class UploadController {

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

        try {
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

        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiResponse.ErrorCode.FILE_UPLOAD_ERROR,
                            "文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 多文件上传
     */
    @PostMapping("/files")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "directory", defaultValue = "files") String directory,
            @Userid Long userId) {

        try {
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

        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiResponse.ErrorCode.FILE_UPLOAD_ERROR,
                            "批量上传失败: " + e.getMessage()));
        }
    }

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            @Userid Long userId) {

        try {
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

        } catch (Exception e) {
            log.error("头像上传失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiResponse.ErrorCode.FILE_UPLOAD_ERROR,
                            "头像上传失败: " + e.getMessage()));
        }
    }

    /**
     * 群组头像上传
     */
    @PostMapping("/group-avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadGroupAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            @RequestParam("groupId") Long groupId,
            @Userid Long userId) {

        try {
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

        } catch (Exception e) {
            log.error("群组头像上传失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiResponse.ErrorCode.FILE_UPLOAD_ERROR,
                            "群组头像上传失败: " + e.getMessage()));
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 检查文件扩展名安全性
        String extension = getFileExtension(filename);
        if (isDangerousExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
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

