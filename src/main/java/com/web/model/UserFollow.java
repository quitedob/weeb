package com.web.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户关注关系实体类
 * 
 * ⚠️ 已废弃 (DEPRECATED) - 2025-11-07
 * 
 * 此模型已废弃，项目采用Contact模型（好友关系，对称关系）
 * 
 * 迁移说明：
 * - 使用 Contact 模型替代 UserFollow
 * - Contact 模型支持对称的好友关系
 * - 好友关系需要双方确认，更符合熟人社交场景
 * 
 * 数据迁移：
 * - 互相关注的用户对应为好友关系（Contact.status = ACCEPTED）
 * - 单向关注关系将被废弃
 * 
 * @see com.web.model.Contact
 */
@Deprecated
@Data
@TableName("user_follow")
public class UserFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long followerId;    // 关注者ID
    private Long followeeId;    // 被关注者ID
    private Date createdAt;     // 创建时间
}