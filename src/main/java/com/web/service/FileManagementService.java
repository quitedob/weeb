package com.web.service;

import com.web.model.FileRecord;
import com.web.vo.file.FileUploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 通用文件管理服务接口
 * 区别于WebRTC文件传输服务
 */
public interface FileManagementService {
    
    /**
     * 上传文件
     * @param file 文件
     * @param userId 用户ID
     * @param isPublic 是否公开
     * @return 文件上传结果
     */
    FileUploadVo uploadFile(MultipartFile file, Long userId, Boolean isPublic);
    
    /**
     * 删除文件
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    void deleteFile(Long fileId, Long userId);
    
    /**
     * 获取用户文件列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表和分页信息
     */
    Map<String, Object> getUserFiles(Long userId, int page, int size);
    
    /**
     * 获取文件详情
     * @param fileId 文件ID
     * @param userId 用户ID（用于权限验证）
     * @return 文件详情
     */
    FileRecord getFileDetails(Long fileId, Long userId);
    
    /**
     * 生成文件访问URL
     * @param fileId 文件ID
     * @return 文件访问URL
     */
    String generateFileUrl(Long fileId);
}