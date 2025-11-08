package com.web.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群组DTO
 * 用于返回群组信息给前端
 */
@Data
public class GroupDto {
    /**
     * 群组ID
     */
    private Long id;
    
    /**
     * 群组名称
     */
    private String groupName;
    
    /**
     * 群组描述
     */
    private String groupDescription;
    
    /**
     * 群主ID
     */
    private Long ownerId;
    
    /**
     * 群主用户名
     */
    private String ownerUsername;
    
    /**
     * 群组头像URL
     */
    private String groupAvatarUrl;
    
    /**
     * 群组状态
     */
    private String status;
    
    /**
     * 最大成员数
     */
    private Integer maxMembers;
    
    /**
     * 当前成员数
     */
    private Integer memberCount;
    
    /**
     * 当前用户在群组中的角色（仅在获取用户群组列表时使用）
     * 1=群主, 2=管理员, 3=普通成员
     */
    private Integer role;
    
    /**
     * 当前用户在群组中的角色名称
     * OWNER=群主, ADMIN=管理员, MEMBER=普通成员, NON_MEMBER=非成员
     */
    private String currentUserRole;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后转让时间
     */
    private LocalDateTime lastTransferAt;
    
    /**
     * 转让次数
     */
    private Integer transferCount;
}
