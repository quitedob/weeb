package com.web.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件分享记录实体类
 * 用于管理文件分享权限
 */
@Data
@TableName("file_share")
public class FileShare {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文件记录ID
     */
    private Long fileId;
    
    /**
     * 分享者用户ID
     */
    private Long sharerId;
    
    /**
     * 被分享者用户ID（可为空，表示公开分享）
     */
    private Long sharedToUserId;
    
    /**
     * 分享链接token
     */
    private String shareToken;
    
    /**
     * 分享权限：READ, DOWNLOAD
     */
    private String permission;
    
    /**
     * 过期时间（可为空，表示永不过期）
     */
    private LocalDateTime expiresAt;
    
    /**
     * 分享状态：ACTIVE, EXPIRED, REVOKED
     */
    private String status;
    
    /**
     * 访问次数
     */
    private Long accessCount;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}