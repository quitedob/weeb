package com.web.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 好友申请DTO
 * 用于返回好友申请列表信息
 */
@Data
public class ContactRequestDto {
    /**
     * 联系人记录ID（用于接受/拒绝操作）
     */
    private Long contactId;
    
    /**
     * 申请人用户ID
     */
    private Long id;
    
    /**
     * 申请人用户名
     */
    private String username;
    
    /**
     * 申请人昵称
     */
    private String nickname;
    
    /**
     * 申请人头像
     */
    private String avatar;
    
    /**
     * 申请备注/附言
     */
    private String remarks;
    
    /**
     * 申请时间
     */
    private LocalDateTime createdAt;
}
