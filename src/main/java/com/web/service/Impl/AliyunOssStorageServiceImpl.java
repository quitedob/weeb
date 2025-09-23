package com.web.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.web.exception.WeebException; // Assuming this custom exception exists
import com.web.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
// Using substring for extension, not FilenameUtils as commons-io is not confirmed.
// import org.apache.commons.io.FilenameUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy; // For cleaning up OSS client
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云OSS存储服务实现
 * 简化注释：阿里云OSS实现
 */
@Service("aliyunOssStorageService") // Specify bean name
@ConditionalOnProperty(
    value = "aliyun.oss.endpoint",
    matchIfMissing = false
)
public class AliyunOssStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssStorageServiceImpl.class);

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;
    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Aliyun OSS Client with endpoint: {}", endpoint);
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            log.info("Aliyun OSS Client initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Aliyun OSS Client: {}", e.getMessage(), e);
            // Depending on policy, could throw an exception to prevent app startup if OSS is critical
            // throw new RuntimeException("Failed to initialize Aliyun OSS Client", e);
        }
    }

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

    @Override
    public String uploadInputStream(InputStream inputStream, String directoryPath, String originalFilename) {
        if (inputStream == null) {
            throw new WeebException("文件输入流不能为空");
        }
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new WeebException("原始文件名不能为空，无法确定文件类型");
        }
        if (ossClient == null) {
            log.error("Aliyun OSS Client not initialized. Cannot upload file.");
            throw new WeebException("存储服务不可用，请稍后再试。");
        }

        // Ensure directoryPath ends with a slash if it's not empty
        String normalizedDirectoryPath = directoryPath.trim();
        if (!normalizedDirectoryPath.isEmpty() && !normalizedDirectoryPath.endsWith("/")) {
            normalizedDirectoryPath += "/";
        }
        if (normalizedDirectoryPath.startsWith("/")) { // Avoid absolute paths if not intended
           normalizedDirectoryPath = normalizedDirectoryPath.substring(1);
        }

        // Generate unique filename: directoryPath/yyyy/MM/dd/uuid.extension
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
        String datePath = sdf.format(new Date());

        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDot); // Includes the dot
        } else if (lastDot == originalFilename.length() -1 ) { // if filename ends with a dot
            extension = ""; // treat as no extension
        }
        // For names without any dot, extension remains ""

        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        String objectName = normalizedDirectoryPath + datePath + newFileName;

        try {
            log.debug("Uploading to OSS. Bucket: {}, ObjectName: {}", bucketName, objectName);
            ossClient.putObject(bucketName, objectName, inputStream);
            log.info("Successfully uploaded to OSS: {}", objectName);
        } catch (Exception e) {
            log.error("上传到OSS失败 for object {}: {}", objectName, e.getMessage(), e);
            throw new WeebException("上传到OSS失败: " + e.getMessage());
        }

        // Construct public URL
        String protocol = "https://";
        String cleanEndpoint = endpoint;
        if (endpoint.startsWith("https://")) {
            cleanEndpoint = endpoint.substring("https://".length());
        } else if (endpoint.startsWith("http://")) {
            cleanEndpoint = endpoint.substring("http://".length());
        }

        // Ensure bucketName is not duplicated if endpoint is already bucket-specific
        // e.g. if endpoint is "my-bucket.oss-cn-hangzhou.aliyuncs.com"
        // then URL should be "https://my-bucket.oss-cn-hangzhou.aliyuncs.com/objectName"
        // if endpoint is "oss-cn-hangzhou.aliyuncs.com"
        // then URL should be "https://my-bucket.oss-cn-hangzhou.aliyuncs.com/objectName"
        if (cleanEndpoint.startsWith(bucketName + ".")) {
             return protocol + cleanEndpoint + "/" + objectName;
        } else {
             return protocol + bucketName + "." + cleanEndpoint + "/" + objectName;
        }
    }

    @PreDestroy // Graceful shutdown of the OSS client
    public void destroy() {
        if (ossClient != null) {
            log.info("Shutting down Aliyun OSS Client.");
            ossClient.shutdown();
        }
    }
}
