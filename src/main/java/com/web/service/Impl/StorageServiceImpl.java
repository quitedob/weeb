package com.web.service.impl;

import com.web.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 存储服务实现类
 * 提供本地文件存储功能，可扩展为云存储服务
 */
@Service
public class StorageServiceImpl implements StorageService {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @Value("${file.upload.base-url:http://localhost:8080/uploads/}")
    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file, String directoryPath) {
        try {
            // 创建目录
            String fullDirectoryPath = uploadPath + directoryPath;
            Path directory = Paths.get(fullDirectoryPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename() + extension;

            // 保存文件
            Path filePath = directory.resolve(uniqueFilename);
            file.transferTo(filePath.toFile());

            // 返回访问URL
            return baseUrl + directoryPath + uniqueFilename;

        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadInputStream(InputStream inputStream, String directoryPath, String originalFilename) {
        try {
            // 创建目录
            String fullDirectoryPath = uploadPath + directoryPath;
            Path directory = Paths.get(fullDirectoryPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // 生成唯一文件名
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename() + extension;

            // 保存文件
            Path filePath = directory.resolve(uniqueFilename);
            try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // 返回访问URL
            return baseUrl + directoryPath + uniqueFilename;

        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFilename() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return timestamp + "_" + uuid;
    }
}