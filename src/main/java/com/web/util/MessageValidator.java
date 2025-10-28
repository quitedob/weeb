package com.web.util;

import com.web.exception.WeebException;
import com.web.vo.message.SendMessageVo;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息验证工具类
 * 统一消息格式验证
 */
@Slf4j
public class MessageValidator {

    // 消息内容最大长度
    private static final int MAX_MESSAGE_LENGTH = 5000;
    
    // 消息内容最小长度
    private static final int MIN_MESSAGE_LENGTH = 1;
    
    // 支持的消息类型
    private static final String[] SUPPORTED_MESSAGE_TYPES = {"PRIVATE", "GROUP"};
    
    // 支持的内容类型
    private static final String[] SUPPORTED_CONTENT_TYPES = {"TEXT", "IMAGE", "FILE", "VIDEO", "AUDIO", "LINK"};

    /**
     * 验证发送消息VO
     * @param sendMessageVo 发送消息VO
     * @throws WeebException 验证失败时抛出异常
     */
    public static void validateSendMessageVo(SendMessageVo sendMessageVo) {
        if (sendMessageVo == null) {
            throw new WeebException("消息对象不能为空");
        }

        // 验证目标类型
        if (sendMessageVo.getTargetType() == null || sendMessageVo.getTargetType().trim().isEmpty()) {
            throw new WeebException("消息目标类型不能为空");
        }

        String targetType = sendMessageVo.getTargetType().toUpperCase();
        boolean validType = false;
        for (String type : SUPPORTED_MESSAGE_TYPES) {
            if (type.equals(targetType)) {
                validType = true;
                break;
            }
        }
        if (!validType) {
            throw new WeebException("不支持的消息类型: " + sendMessageVo.getTargetType());
        }

        // 验证目标ID
        if (sendMessageVo.getTargetId() == null || sendMessageVo.getTargetId() <= 0) {
            throw new WeebException("消息目标ID无效");
        }

        // 验证消息内容
        Object content = sendMessageVo.getContent();
        if (content instanceof String) {
            validateMessageContent((String) content);
        } else if (content != null) {
            // 对于非String类型的content，转换为String进行验证
            validateMessageContent(content.toString());
        } else {
            throw new WeebException("消息内容不能为空");
        }
    }

    /**
     * 验证消息内容
     * @param content 消息内容
     * @throws WeebException 验证失败时抛出异常
     */
    public static void validateMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new WeebException("消息内容不能为空");
        }

        if (content.length() < MIN_MESSAGE_LENGTH) {
            throw new WeebException("消息内容过短");
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new WeebException("消息内容过长，最多" + MAX_MESSAGE_LENGTH + "个字符");
        }

        // 检查是否包含非法字符
        if (containsIllegalCharacters(content)) {
            throw new WeebException("消息内容包含非法字符");
        }
    }

    /**
     * 验证消息ID
     * @param messageId 消息ID
     * @throws WeebException 验证失败时抛出异常
     */
    public static void validateMessageId(Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw new WeebException("消息ID无效");
        }
    }

    /**
     * 验证用户ID
     * @param userId 用户ID
     * @throws WeebException 验证失败时抛出异常
     */
    public static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID无效");
        }
    }

    /**
     * 验证分页参数
     * @param page 页码
     * @param size 每页大小
     * @throws WeebException 验证失败时抛出异常
     */
    public static void validatePagination(int page, int size) {
        if (page < 1) {
            throw new WeebException("页码必须大于0");
        }

        if (size < 1 || size > 100) {
            throw new WeebException("每页大小必须在1-100之间");
        }
    }

    /**
     * 验证搜索关键词
     * @param keyword 关键词
     * @throws WeebException 验证失败时抛出异常
     */
    public static void validateSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new WeebException("搜索关键词不能为空");
        }

        if (keyword.length() > 100) {
            throw new WeebException("搜索关键词过长，最多100个字符");
        }
    }

    /**
     * 检查内容是否包含非法字符
     * @param content 内容
     * @return 是否包含非法字符
     */
    private static boolean containsIllegalCharacters(String content) {
        // 检查是否包含控制字符（除了换行、回车、制表符）
        for (char c : content.toCharArray()) {
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理消息内容
     * @param content 原始内容
     * @return 清理后的内容
     */
    public static String sanitizeContent(String content) {
        if (content == null) {
            return "";
        }

        // 移除首尾空白
        content = content.trim();

        // 移除多余的空行（连续超过2个换行符）
        content = content.replaceAll("(\r?\n){3,}", "\n\n");

        // 移除非法字符
        StringBuilder sb = new StringBuilder();
        for (char c : content.toCharArray()) {
            if (!Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t') {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 验证内容类型
     * @param contentType 内容类型
     * @throws WeebException 验证失败时抛出异常
     */
    public static void validateContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new WeebException("内容类型不能为空");
        }

        String type = contentType.toUpperCase();
        boolean validType = false;
        for (String supportedType : SUPPORTED_CONTENT_TYPES) {
            if (supportedType.equals(type)) {
                validType = true;
                break;
            }
        }

        if (!validType) {
            throw new WeebException("不支持的内容类型: " + contentType);
        }
    }
}
