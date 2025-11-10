package com.web.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一聊天列表数据传输对象
 * 提供标准化的API响应格式，解决前后端字段不一致问题
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatListDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 聊天记录ID（对应数据库 id 字段）
     */
    private String id;

    /**
     * 用户ID（对应数据库 user_id 字段）
     */
    private Long userId;

    /**
     * 共享聊天ID（对应数据库 shared_chat_id 字段）
     * 私聊双方共用，群聊所有成员共用
     */
    private Long sharedChatId;

    /**
     * 目标用户ID（对应数据库 target_id 字段，私聊时使用）
     */
    private Long targetId;

    /**
     * 目标信息（对应数据库 target_info 字段）
     * 私聊时为JSON格式的用户信息，群聊时为群组名称
     */
    private String targetInfo;

    /**
     * 未读消息数（对应数据库 unread_count 字段）
     */
    private Integer unreadCount;

    /**
     * 最后一条消息（对应数据库 last_message 字段）
     */
    private String lastMessage;

    /**
     * 会话类型（对应数据库 type 字段）
     * PRIVATE: 私聊, GROUP: 群聊
     */
    private String type;

    /**
     * 创建时间（对应数据库 create_time 字段）
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间（对应数据库 update_time 字段）
     */
    private LocalDateTime updateTime;

    /**
     * 群组ID（对应数据库 group_id 字段，群聊时使用）
     */
    private Long groupId;

    /**
     * 群组名称（群聊时的额外字段，从targetInfo或群组信息获取）
     */
    private String groupName;

    /**
     * 目标用户信息解析后的对象（私聊时的额外字段）
     */
    private TargetUserInfo targetUserInfo;

    /**
     * 目标用户信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetUserInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String username;
        private String name;
        private String avatar;
    }

    /**
     * 从ChatList实体转换到DTO
     * @param chatList ChatList实体
     * @return ChatListDTO
     */
    public static ChatListDTO fromEntity(com.web.model.ChatList chatList) {
        if (chatList == null) {
            return null;
        }

        ChatListDTO dto = new ChatListDTO();
        dto.setId(chatList.getId());
        dto.setUserId(chatList.getUserId());
        dto.setSharedChatId(chatList.getSharedChatId());
        dto.setTargetId(chatList.getTargetId());
        dto.setTargetInfo(chatList.getTargetInfo());
        dto.setUnreadCount(chatList.getUnreadCount());
        dto.setLastMessage(chatList.getLastMessage());
        dto.setType(chatList.getType());
        dto.setCreateTime(chatList.getCreateTime());
        dto.setUpdateTime(chatList.getUpdateTime());
        dto.setGroupId(chatList.getGroupId());

        // 解析targetInfo
        if (chatList.getTargetInfo() != null) {
            try {
                // 尝试解析为JSON对象
                if ("PRIVATE".equals(chatList.getType())) {
                    // 私聊时targetInfo应该是JSON格式的用户信息
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    TargetUserInfo userInfo = mapper.readValue(chatList.getTargetInfo(), TargetUserInfo.class);
                    dto.setTargetUserInfo(userInfo);
                } else if ("GROUP".equals(chatList.getType())) {
                    // 群聊时targetInfo是群组名称
                    dto.setGroupName(chatList.getTargetInfo());
                }
            } catch (Exception e) {
                // 解析失败时，将原始字符串作为名称
                if ("GROUP".equals(chatList.getType())) {
                    dto.setGroupName(chatList.getTargetInfo());
                }
                // 私聊时保持原始JSON字符串
            }
        }

        return dto;
    }

    /**
     * 获取显示名称
     * 私聊时返回用户名，群聊时返回群组名称
     * @return 显示名称
     */
    public String getDisplayName() {
        if ("GROUP".equals(type)) {
            return groupName != null ? groupName : targetInfo;
        } else if ("PRIVATE".equals(type)) {
            if (targetUserInfo != null) {
                return targetUserInfo.getName() != null ? targetUserInfo.getName() : targetUserInfo.getUsername();
            }
            return targetInfo; // 兜底：返回原始字符串
        }
        return targetInfo;
    }

    /**
     * 获取显示头像
     * @return 头像URL，群聊时返回null
     */
    public String getDisplayAvatar() {
        if ("PRIVATE".equals(type) && targetUserInfo != null) {
            return targetUserInfo.getAvatar();
        }
        return null;
    }
}