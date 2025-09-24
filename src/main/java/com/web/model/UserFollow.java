package com.web.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户关注关系实体类
 */
@Data
@TableName("user_follow")
public class UserFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long followerId;    // 关注者ID
    private Long followeeId;    // 被关注者ID
    private Date createdAt;     // 创建时间
}