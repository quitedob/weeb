package com.web.Controller;

import com.web.annotation.UrlLimit;
import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.FileRecord;
import com.web.service.FileManagementService;
import com.web.vo.file.FileUploadVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 文件管理控制器
 * 提供文件上传、下载、删除等功能
 */
@RestController
@RequestMapping("/api/files")
public class FileManagementController {

    @Autowired
    private FileManagementService fileManagementService;

    /**
     * 上传文件
     * @param file 文件
     * @param isPublic 是否公开
     * @param userId 用户ID
     * @return 上传结果
     */
    @UrlLimit
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadVo>> uploadFile(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
            @Userid Long userId) {

        FileUploadVo result = fileManagementService.uploadFile(file, userId, isPublic);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除文件
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @UrlLimit
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable Long fileId,
            @Userid Long userId) {

        fileManagementService.deleteFile(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success("文件已删除"));
    }

    /**
     * 获取用户文件列表
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID
     * @return 文件列表
     */
    @UrlLimit
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Userid Long userId) {

        Map<String, Object> result = fileManagementService.getUserFiles(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取文件详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件详情
     */
    @UrlLimit
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileRecord>> getFileDetails(
            @PathVariable Long fileId,
            @Userid Long userId) {

        FileRecord file = fileManagementService.getFileDetails(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success(file));
    }
}