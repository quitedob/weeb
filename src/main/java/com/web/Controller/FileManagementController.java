package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.FileRecord;
import com.web.service.FileManagementService;
import com.web.vo.file.FileUploadVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 通用文件管理控制器
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
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadVo>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPublic", defaultValue = "false") Boolean isPublic,
            @Userid Long userId) {
        FileUploadVo result = fileManagementService.uploadFile(file, userId, isPublic);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除文件
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 操作结果
     */
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
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Userid Long userId) {
        Map<String, Object> files = fileManagementService.getUserFiles(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    /**
     * 下载文件
     * @param fileId 文件ID
     * @return 文件资源
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        try {
            // 对于下载，我们允许匿名访问公开文件
            FileRecord fileRecord = fileManagementService.getFileDetails(fileId, null);
            
            File file = new File(fileRecord.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            
            // 设置响应头
            String encodedFileName = URLEncoder.encode(fileRecord.getFileName(), StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + encodedFileName + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, fileRecord.getMimeType())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileRecord.getFileSize()))
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取文件详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件详情
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileRecord>> getFileDetails(
            @PathVariable Long fileId,
            @Userid Long userId) {
        FileRecord fileRecord = fileManagementService.getFileDetails(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success(fileRecord));
    }
}