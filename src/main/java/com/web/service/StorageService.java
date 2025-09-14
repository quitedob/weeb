package com.web.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

/**
 * 对象存储服务接口
 * 定义了文件上传和管理的基本操作，以便于切换不同的云服务商
 * 简化注释：存储服务接口
 */
public interface StorageService {

    /**
     * 上传文件
     * @param file Spring的MultipartFile对象
     * @param directoryPath 文件在OSS中的目录路径, e.g., "images/avatars/"
     * @return 文件的公共访问URL
     * 简化注释：上传文件
     */
    String uploadFile(MultipartFile file, String directoryPath);

    /**
     * 上传输入流
     * @param inputStream 文件输入流
     * @param directoryPath 目录路径
     * @param originalFilename 原始文件名 (to extract extension and for metadata)
     * @return 文件的公共访问URL
     * 简化注释：上传输入流
     */
    String uploadInputStream(InputStream inputStream, String directoryPath, String originalFilename);
}
