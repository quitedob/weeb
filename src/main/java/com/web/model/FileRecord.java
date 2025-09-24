package com.web.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件记录实体类
 * 用于通用文件管理（区别于WebRTC文件传输）
 */
@Data
@TableName("file_record")
public class FileRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;        // 上传者ID
    private String fileName;    // 原始文件名
    private String storedName;  // 存储文件名
    private String filePath;    // 文件路径
    private Long fileSize;      // 文件大小（字节）
    private String mimeType;    // MIME类型
    private String fileHash;    // 文件哈希值（用于去重）
    private Boolean isPublic;   // 是否公开
    private Date createdAt;     // 创建时间
    private Date updatedAt;     // 更新时间
}