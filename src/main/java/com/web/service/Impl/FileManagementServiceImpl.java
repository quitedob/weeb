package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.FileRecordMapper;
import com.web.model.FileRecord;
import com.web.service.FileManagementService;
import com.web.vo.file.FileUploadVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 通用文件管理服务实现类
 */
@Service
@Transactional
public class FileManagementServiceImpl implements FileManagementService {

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.max.size:10485760}") // 10MB
    private long maxFileSize;

    @Override
    public FileUploadVo uploadFile(MultipartFile file, Long userId, Boolean isPublic) {
        if (file.isEmpty()) {
            throw new WeebException("文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new WeebException("文件大小不能超过10MB");
        }

        try {
            // 计算文件哈希值
            String fileHash = calculateFileHash(file.getBytes());
            
            // 检查是否已存在相同文件
            FileRecord existingFile = fileRecordMapper.findByHash(fileHash);
            if (existingFile != null) {
                // 文件已存在，直接返回现有文件信息
                FileUploadVo uploadVo = new FileUploadVo();
                uploadVo.setFileId(existingFile.getId());
                uploadVo.setFileName(existingFile.getFileName());
                uploadVo.setFileUrl(generateFileUrl(existingFile.getId()));
                uploadVo.setFileSize(existingFile.getFileSize());
                uploadVo.setMimeType(existingFile.getMimeType());
                return uploadVo;
            }

            // 创建上传目录
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成存储文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, storedName);
            Files.write(filePath, file.getBytes());

            // 保存文件记录
            FileRecord fileRecord = new FileRecord();
            fileRecord.setUserId(userId);
            fileRecord.setFileName(originalFilename);
            fileRecord.setStoredName(storedName);
            fileRecord.setFilePath(filePath.toString());
            fileRecord.setFileSize(file.getSize());
            fileRecord.setMimeType(file.getContentType());
            fileRecord.setFileHash(fileHash);
            fileRecord.setIsPublic(isPublic != null ? isPublic : false);
            fileRecord.setCreatedAt(new Date());
            fileRecord.setUpdatedAt(new Date());

            fileRecordMapper.insert(fileRecord);

            // 构建返回结果
            FileUploadVo uploadVo = new FileUploadVo();
            uploadVo.setFileId(fileRecord.getId());
            uploadVo.setFileName(fileRecord.getFileName());
            uploadVo.setFileUrl(generateFileUrl(fileRecord.getId()));
            uploadVo.setFileSize(fileRecord.getFileSize());
            uploadVo.setMimeType(fileRecord.getMimeType());

            return uploadVo;

        } catch (IOException e) {
            throw new WeebException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(Long fileId, Long userId) {
        FileRecord fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            throw new WeebException("文件不存在");
        }

        if (!fileRecord.getUserId().equals(userId)) {
            throw new WeebException("只能删除自己的文件");
        }

        // 删除物理文件
        try {
            Path filePath = Paths.get(fileRecord.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("删除物理文件失败: " + e.getMessage());
        }

        // 删除数据库记录
        fileRecordMapper.deleteById(fileId);
    }

    @Override
    public Map<String, Object> getUserFiles(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        
        List<Map<String, Object>> files = fileRecordMapper.getUserFiles(userId, offset, size);
        int total = fileRecordMapper.getUserFileCount(userId);

        // 为每个文件添加访问URL
        for (Map<String, Object> file : files) {
            Long fileId = (Long) file.get("id");
            file.put("fileUrl", generateFileUrl(fileId));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("files", files);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));

        return result;
    }

    @Override
    public FileRecord getFileDetails(Long fileId, Long userId) {
        FileRecord fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            throw new WeebException("文件不存在");
        }

        // 检查访问权限
        if (!fileRecord.getIsPublic()) {
            // 如果文件不是公开的，需要验证用户权限
            if (userId == null || !fileRecord.getUserId().equals(userId)) {
                throw new WeebException("无权访问此文件");
            }
        }

        return fileRecord;
    }

    @Override
    public String generateFileUrl(Long fileId) {
        return "/api/files/download/" + fileId;
    }

    /**
     * 计算文件哈希值
     */
    private String calculateFileHash(byte[] fileBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new WeebException("计算文件哈希值失败");
        }
    }
}