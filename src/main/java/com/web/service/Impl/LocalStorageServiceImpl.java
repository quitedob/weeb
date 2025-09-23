package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 本地文件存储服务实现
 * 用于替代阿里云OSS的本地文件存储解决方案
 */
@Service
@Slf4j
public class LocalStorageServiceImpl implements StorageService {

    @Value("${weeb.upload.base-path:uploads}")
    private String baseUploadPath;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 上传MultipartFile文件
     */
    @Override
    public String uploadFile(MultipartFile file, String directoryPath) {
        if (file == null || file.isEmpty()) {
            throw new WeebException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new WeebException("文件名不能为空");
        }

        try {
            return uploadInputStream(file.getInputStream(), directoryPath, originalFilename);
        } catch (Exception e) {
            log.error("文件上传失败 (MultipartFile): {}", e.getMessage(), e);
            throw new WeebException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传输入流文件
     */
    @Override
    public String uploadInputStream(InputStream inputStream, String directoryPath, String originalFilename) {
        if (inputStream == null) {
            throw new WeebException("文件输入流不能为空");
        }
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new WeebException("原始文件名不能为空，无法确定文件类型");
        }

        try {
            // 创建基础上传目录
            Path basePath = Paths.get(baseUploadPath);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("创建上传基础目录: {}", basePath.toAbsolutePath());
            }

            // 处理目录路径
            String normalizedDirectoryPath = directoryPath.trim();
            if (!normalizedDirectoryPath.isEmpty() && !normalizedDirectoryPath.endsWith("/")) {
                normalizedDirectoryPath += "/";
            }
            if (normalizedDirectoryPath.startsWith("/")) {
                normalizedDirectoryPath = normalizedDirectoryPath.substring(1);
            }

            // 生成日期路径
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
            String datePath = sdf.format(new Date());

            // 获取文件扩展名
            String extension = "";
            int lastDot = originalFilename.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < originalFilename.length() - 1) {
                extension = originalFilename.substring(lastDot);
            }

            // 生成唯一文件名
            String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
            String relativePath = normalizedDirectoryPath + datePath + newFileName;

            // 创建完整文件路径
            Path filePath = basePath.resolve(relativePath);
            Files.createDirectories(filePath.getParent());

            // 保存文件
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件上传成功: {}", filePath.toAbsolutePath());

            // 返回可访问的URL
            String cleanContextPath = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
            return "http://localhost:" + serverPort + cleanContextPath + "/files/" + relativePath.replace("\\", "/");

        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new WeebException("文件保存失败: " + e.getMessage());
        }
    }
}
