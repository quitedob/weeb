package com.web.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 联系人DTO
 * 用于返回联系人信息给前端
 */
@Data
public class ContactDto {
    /**
     * 联系人记录ID（用于删除等操作）
     */
    private Long contactId;
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 联系时间（成为好友的时间）
     */
    private LocalDateTime contactTime;
}