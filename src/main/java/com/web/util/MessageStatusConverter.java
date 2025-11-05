package com.web.util;

import com.web.model.Message;

/**
 * 消息状态转换工具类
 * 用于前后端消息状态的统一转换
 */
public class MessageStatusConverter {
    
    /**
     * 将旧的readStatus和isRecalled转换为新的status
     * @param readStatus 旧的已读状态
     * @param isRecalled 是否撤回
     * @return 新的统一状态
     */
    public static Integer convertLegacyToStatus(Integer readStatus, Integer isRecalled) {
        // 如果已撤回，状态不适用（由isRecalled字段单独处理）
        if (isRecalled != null && isRecalled == 1) {
            return Message.STATUS_SENT; // 撤回的消息保持已发送状态
        }
        
        // 根据readStatus转换
        if (readStatus == null || readStatus == 0) {
            return Message.STATUS_SENT; // 未读 -> 已发送
        } else {
            return Message.STATUS_READ; // 已读 -> 已读
        }
    }
    
    /**
     * 将新的status转换为旧的readStatus（向后兼容）
     * @param status 新的统一状态
     * @return 旧的已读状态
     */
    public static Integer convertStatusToLegacyReadStatus(Integer status) {
        if (status == null) {
            return 0;
        }
        
        return switch (status) {
            case Message.STATUS_SENDING, Message.STATUS_SENT, Message.STATUS_DELIVERED, Message.STATUS_FAILED -> 0; // 未读
            case Message.STATUS_READ -> 1; // 已读
            default -> 0;
        };
    }
    
    /**
     * 获取状态的文本描述
     * @param status 状态码
     * @return 状态描述
     */
    public static String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        
        return switch (status) {
            case Message.STATUS_SENDING -> "发送中";
            case Message.STATUS_SENT -> "已发送";
            case Message.STATUS_DELIVERED -> "已送达";
            case Message.STATUS_READ -> "已读";
            case Message.STATUS_FAILED -> "发送失败";
            default -> "未知";
        };
    }
    
    /**
     * 获取状态的英文描述（用于前端）
     * @param status 状态码
     * @return 状态英文描述
     */
    public static String getStatusTextEn(Integer status) {
        if (status == null) {
            return "unknown";
        }
        
        return switch (status) {
            case Message.STATUS_SENDING -> "sending";
            case Message.STATUS_SENT -> "sent";
            case Message.STATUS_DELIVERED -> "delivered";
            case Message.STATUS_READ -> "read";
            case Message.STATUS_FAILED -> "failed";
            default -> "unknown";
        };
    }
    
    /**
     * 从英文状态描述转换为状态码
     * @param statusText 英文状态描述
     * @return 状态码
     */
    public static Integer parseStatusText(String statusText) {
        if (statusText == null) {
            return Message.STATUS_SENT;
        }
        
        return switch (statusText.toLowerCase()) {
            case "sending" -> Message.STATUS_SENDING;
            case "sent" -> Message.STATUS_SENT;
            case "delivered" -> Message.STATUS_DELIVERED;
            case "read" -> Message.STATUS_READ;
            case "failed" -> Message.STATUS_FAILED;
            default -> Message.STATUS_SENT;
        };
    }
}
