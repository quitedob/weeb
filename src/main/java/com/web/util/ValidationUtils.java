package com.web.util;

import com.web.Config.SecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 输入验证工具类
 * 提供各种输入验证方法，确保数据安全性
 */
@Component
@Slf4j
public class ValidationUtils {

    /**
     * 验证用户名
     */
    public static boolean validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("用户名验证失败：用户名为空");
            return false;
        }
        
        String trimmedUsername = username.trim();
        if (trimmedUsername.length() < SecurityConfig.UsernamePolicy.MIN_LENGTH || 
            trimmedUsername.length() > SecurityConfig.UsernamePolicy.MAX_LENGTH) {
            log.warn("用户名验证失败：长度不符合要求 - {}", trimmedUsername.length());
            return false;
        }
        
        if (!trimmedUsername.matches(SecurityConfig.UsernamePolicy.PATTERN)) {
            log.warn("用户名验证失败：格式不符合要求 - {}", trimmedUsername);
            return false;
        }
        
        return true;
    }

    /**
     * 验证密码
     */
    public static boolean validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            log.warn("密码验证失败：密码为空");
            return false;
        }
        
        String trimmedPassword = password.trim();
        if (trimmedPassword.length() < SecurityConfig.PasswordPolicy.MIN_LENGTH || 
            trimmedPassword.length() > SecurityConfig.PasswordPolicy.MAX_LENGTH) {
            log.warn("密码验证失败：长度不符合要求 - {}", trimmedPassword.length());
            return false;
        }
        
        if (!trimmedPassword.matches(SecurityConfig.PasswordPolicy.PATTERN)) {
            log.warn("密码验证失败：强度不符合要求");
            return false;
        }
        
        return true;
    }

    /**
     * 验证邮箱
     */
    public static boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("邮箱验证失败：邮箱为空");
            return false;
        }
        
        String trimmedEmail = email.trim();
        if (!trimmedEmail.matches(SecurityConfig.EmailPolicy.PATTERN)) {
            log.warn("邮箱验证失败：格式不符合要求 - {}", trimmedEmail);
            return false;
        }
        
        return true;
    }

    /**
     * 验证手机号
     */
    public static boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("手机号验证失败：手机号为空");
            return false;
        }
        
        String trimmedPhone = phone.trim();
        if (!trimmedPhone.matches(SecurityConfig.PhonePolicy.PATTERN)) {
            log.warn("手机号验证失败：格式不符合要求 - {}", trimmedPhone);
            return false;
        }
        
        return true;
    }

    /**
     * 验证评论内容
     */
    public static boolean validateCommentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("评论内容验证失败：内容为空");
            return false;
        }
        
        String trimmedContent = content.trim();
        if (trimmedContent.length() > 1000) {
            log.warn("评论内容验证失败：内容过长 - {}", trimmedContent.length());
            return false;
        }
        
        // 检查是否包含敏感词
        if (containsSensitiveWords(trimmedContent)) {
            log.warn("评论内容验证失败：包含敏感词");
            return false;
        }
        
        return true;
    }

    /**
     * 验证文章标题
     */
    public static boolean validateArticleTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            log.warn("文章标题验证失败：标题为空");
            return false;
        }
        
        String trimmedTitle = title.trim();
        if (trimmedTitle.length() < 1 || trimmedTitle.length() > 200) {
            log.warn("文章标题验证失败：长度不符合要求 - {}", trimmedTitle.length());
            return false;
        }
        
        // 检查是否包含敏感词
        if (containsSensitiveWords(trimmedTitle)) {
            log.warn("文章标题验证失败：包含敏感词");
            return false;
        }
        
        return true;
    }

    /**
     * 验证文章内容
     */
    public static boolean validateArticleContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("文章内容验证失败：内容为空");
            return false;
        }
        
        String trimmedContent = content.trim();
        if (trimmedContent.length() > 10000) {
            log.warn("文章内容验证失败：内容过长 - {}", trimmedContent.length());
            return false;
        }
        
        // 检查是否包含敏感词
        if (containsSensitiveWords(trimmedContent)) {
            log.warn("文章内容验证失败：包含敏感词");
            return false;
        }
        
        return true;
    }

    /**
     * 检查是否包含敏感词
     */
    private static boolean containsSensitiveWords(String content) {
        // 简单的敏感词检查，实际项目中应该使用更完善的敏感词库
        String[] sensitiveWords = {
            "垃圾", "诈骗", "赌博", "色情", "暴力", "毒品", "恐怖主义",
            "政治敏感", "违法", "违规", "不良信息"
        };
        
        String lowerContent = content.toLowerCase();
        for (String word : sensitiveWords) {
            if (lowerContent.contains(word)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 验证ID参数
     */
    public static boolean validateId(Long id, String paramName) {
        if (id == null || id <= 0) {
            log.warn("{}验证失败：ID无效 - {}", paramName, id);
            return false;
        }
        return true;
    }

    /**
     * 验证分页参数
     */
    public static boolean validatePageParams(Integer page, Integer pageSize, String paramName) {
        if (page == null || page < 1) {
            log.warn("{}分页参数验证失败：页码无效 - {}", paramName, page);
            return false;
        }
        
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            log.warn("{}分页参数验证失败：页大小无效 - {}", paramName, pageSize);
            return false;
        }
        
        return true;
    }

    /**
     * 清理和验证字符串输入
     */
    public static String cleanAndValidateString(String input, String paramName, int maxLength) {
        if (input == null) {
            log.warn("{}验证失败：输入为空", paramName);
            return null;
        }
        
        String cleaned = input.trim();
        if (cleaned.length() > maxLength) {
            log.warn("{}验证失败：输入过长 - {}", paramName, cleaned.length());
            return null;
        }
        
        // 检查是否包含潜在的XSS攻击字符
        if (containsXSSPatterns(cleaned)) {
            log.warn("{}验证失败：包含潜在的XSS攻击字符 - {}", paramName, cleaned);
            return null;
        }
        
        return cleaned;
    }

    /**
     * 检查是否包含潜在的XSS攻击字符
     */
    private static boolean containsXSSPatterns(String input) {
        String[] xssPatterns = {
            "<script", "</script>", "javascript:", "vbscript:", "onload=", "onerror=",
            "onclick=", "onmouseover=", "onmouseout=", "onfocus=", "onblur=",
            "eval(", "expression(", "import ", "from ", "alert(", "confirm(",
            "document.cookie", "window.location", "document.write"
        };
        
        String lowerInput = input.toLowerCase();
        for (String pattern : xssPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 验证文件上传
     */
    public static boolean validateFileUpload(String fileName, long fileSize, long maxSize) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("文件上传验证失败：文件名为空");
            return false;
        }

        // 检查文件扩展名
        String lowerFileName = fileName.toLowerCase();
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".pdf", ".doc", ".docx", ".txt", ".zip", ".rar"};
        boolean hasValidExtension = false;
        for (String ext : allowedExtensions) {
            if (lowerFileName.endsWith(ext)) {
                hasValidExtension = true;
                break;
            }
        }

        if (!hasValidExtension) {
            log.warn("文件上传验证失败：文件扩展名不允许 - {}", fileName);
            return false;
        }

        // 检查文件大小
        if (fileSize > maxSize) {
            log.warn("文件上传验证失败：文件大小过大 - {} bytes", fileSize);
            return false;
        }

        return true;
    }

    /**
     * 验证聊天消息内容
     */
    public static boolean validateChatMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("聊天消息验证失败：内容为空");
            return false;
        }

        String trimmedContent = content.trim();
        if (trimmedContent.length() > 2000) {
            log.warn("聊天消息验证失败：内容过长 - {}", trimmedContent.length());
            return false;
        }

        // 检查是否包含敏感词
        if (containsSensitiveWords(trimmedContent)) {
            log.warn("聊天消息验证失败：包含敏感词");
            return false;
        }

        return true;
    }

    /**
     * 验证群组名称
     */
    public static boolean validateGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            log.warn("群组名称验证失败：群组名称为空");
            return false;
        }

        String trimmedGroupName = groupName.trim();
        if (trimmedGroupName.length() < 2 || trimmedGroupName.length() > 50) {
            log.warn("群组名称验证失败：长度不符合要求 - {}", trimmedGroupName.length());
            return false;
        }

        // 检查是否包含敏感词
        if (containsSensitiveWords(trimmedGroupName)) {
            log.warn("群组名称验证失败：包含敏感词");
            return false;
        }

        return true;
    }

    /**
     * 验证群组描述
     */
    public static boolean validateGroupDescription(String description) {
        if (description == null) {
            return true; // 描述是可选的
        }

        String trimmedDescription = description.trim();
        if (trimmedDescription.length() > 500) {
            log.warn("群组描述验证失败：描述过长 - {}", trimmedDescription.length());
            return false;
        }

        // 检查是否包含敏感词
        if (containsSensitiveWords(trimmedDescription)) {
            log.warn("群组描述验证失败：包含敏感词");
            return false;
        }

        return true;
    }

    /**
     * 验证搜索关键词
     */
    public static boolean validateSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("搜索关键词验证失败：关键词为空");
            return false;
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.length() < 1 || trimmedKeyword.length() > 100) {
            log.warn("搜索关键词验证失败：长度不符合要求 - {}", trimmedKeyword.length());
            return false;
        }

        // 检查是否包含敏感词
        if (containsSensitiveWords(trimmedKeyword)) {
            log.warn("搜索关键词验证失败：包含敏感词");
            return false;
        }

        return true;
    }

    /**
     * 验证用户昵称
     */
    public static boolean validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return true; // 昵称是可选的
        }

        String trimmedNickname = nickname.trim();
        if (trimmedNickname.length() > 50) {
            log.warn("用户昵称验证失败：昵称过长 - {}", trimmedNickname.length());
            return false;
        }

        // 检查是否包含敏感词
        if (containsSensitiveWords(trimmedNickname)) {
            log.warn("用户昵称验证失败：包含敏感词");
            return false;
        }

        return true;
    }

    /**
     * 验证日期范围
     */
    public static boolean validateDateRange(String startDate, String endDate) {
        if (startDate == null || startDate.trim().isEmpty()) {
            return true; // 开始日期是可选的
        }

        if (endDate == null || endDate.trim().isEmpty()) {
            return true; // 结束日期是可选的
        }

        // 简单的日期格式验证 (YYYY-MM-DD)
        String datePattern = "^\\d{4}-\\d{2}-\\d{2}$";
        if (!startDate.matches(datePattern) || !endDate.matches(datePattern)) {
            log.warn("日期范围验证失败：日期格式不正确 - startDate: {}, endDate: {}", startDate, endDate);
            return false;
        }

        // 检查开始日期是否早于或等于结束日期
        if (startDate.compareTo(endDate) > 0) {
            log.warn("日期范围验证失败：开始日期晚于结束日期 - startDate: {}, endDate: {}", startDate, endDate);
            return false;
        }

        return true;
    }

    /**
     * 验证消息类型
     */
    public static boolean validateMessageType(Integer messageType) {
        if (messageType == null) {
            log.warn("消息类型验证失败：消息类型为空");
            return false;
        }

        // 允许的消息类型：1-文本消息，2-文件消息，3-图片消息
        if (messageType < 1 || messageType > 3) {
            log.warn("消息类型验证失败：消息类型无效 - {}", messageType);
            return false;
        }

        return true;
    }

    /**
     * 验证文件名
     */
    public static boolean validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("文件名验证失败：文件名为空");
            return false;
        }

        String cleanFileName = fileName.trim();

        // 检查文件名长度
        if (cleanFileName.length() > 255) {
            log.warn("文件名验证失败：文件名过长 - {}", cleanFileName.length());
            return false;
        }

        // 检查路径遍历攻击
        if (cleanFileName.contains("../") || cleanFileName.contains("..\\")) {
            log.warn("文件名验证失败：包含路径遍历字符 - {}", cleanFileName);
            return false;
        }

        // 检查是否包含危险字符
        String[] dangerousChars = {"<", ">", ":", "\"", "|", "?", "*", "\0"};
        for (String dangerousChar : dangerousChars) {
            if (cleanFileName.contains(dangerousChar)) {
                log.warn("文件名验证失败：包含危险字符 '{}' - {}", dangerousChar, cleanFileName);
                return false;
            }
        }

        // 检查是否为保留名称（Windows）
        String[] reservedNames = {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        };

        String nameWithoutExtension = cleanFileName.contains(".")
            ? cleanFileName.substring(0, cleanFileName.lastIndexOf('.')).toUpperCase()
            : cleanFileName.toUpperCase();

        for (String reservedName : reservedNames) {
            if (nameWithoutExtension.equals(reservedName)) {
                log.warn("文件名验证失败：使用保留名称 - {}", cleanFileName);
                return false;
            }
        }

        return true;
    }

    /**
     * 验证排序参数
     */
    public static boolean validateSortBy(String sortBy, String[] allowedValues) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return true; // 排序参数是可选的
        }

        String trimmedSortBy = sortBy.trim();
        for (String allowedValue : allowedValues) {
            if (allowedValue.equals(trimmedSortBy)) {
                return true;
            }
        }

        log.warn("排序参数验证失败：不允许的排序值 - {}", sortBy);
        return false;
    }

    /**
     * 验证批量ID列表
     */
    public static boolean validateIdList(String idList, String paramName) {
        if (idList == null || idList.trim().isEmpty()) {
            return true; // ID列表是可选的
        }

        String trimmedIdList = idList.trim();
        String[] ids = trimmedIdList.split(",");

        for (String idStr : ids) {
            try {
                Long id = Long.parseLong(idStr.trim());
                if (id <= 0) {
                    log.warn("{}验证失败：包含无效ID - {}", paramName, idStr);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.warn("{}验证失败：ID格式错误 - {}", paramName, idStr);
                return false;
            }
        }

        return true;
    }
}
