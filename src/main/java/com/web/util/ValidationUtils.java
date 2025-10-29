package com.web.util;

import com.web.Config.SecurityConstants;
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
        if (trimmedUsername.length() < SecurityConstants.UsernamePolicy.MIN_LENGTH ||
            trimmedUsername.length() > SecurityConstants.UsernamePolicy.MAX_LENGTH) {
            log.warn("用户名验证失败：长度不符合要求 - {}", trimmedUsername.length());
            return false;
        }
        
        if (!trimmedUsername.matches(SecurityConstants.UsernamePolicy.PATTERN)) {
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
        if (trimmedPassword.length() < SecurityConstants.PasswordPolicy.MIN_LENGTH ||
            trimmedPassword.length() > SecurityConstants.PasswordPolicy.MAX_LENGTH) {
            log.warn("密码验证失败：长度不符合要求 - {}", trimmedPassword.length());
            return false;
        }
        
        if (!trimmedPassword.matches(SecurityConstants.PasswordPolicy.PATTERN)) {
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
        if (!trimmedEmail.matches(SecurityConstants.EmailPolicy.PATTERN)) {
            log.warn("邮箱验证失败：格式不符合要求 - {}", trimmedEmail);
            return false;
        }
        
        return true;
    }

    /**
     * 验证手机号
     * 支持国际区号格式（如+8613800138000）和纯中国手机号格式（如13800138000）
     */
    public static boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("手机号验证失败：手机号为空");
            return false;
        }

        String trimmedPhone = phone.trim();

        // 如果包含国际区号，去掉区号后验证中国手机号
        if (trimmedPhone.startsWith("+")) {
            // 支持中国区号 +86
            if (trimmedPhone.startsWith("+86")) {
                String chinesePhone = trimmedPhone.substring(3); // 去掉+86
                if (!chinesePhone.matches(SecurityConstants.PhonePolicy.PATTERN)) {
                    log.warn("手机号验证失败：中国手机号格式不符合要求 - {}", trimmedPhone);
                    return false;
                }
            } else {
                // 其他国家的基本验证（至少6位数字）
                String phoneNumber = trimmedPhone.substring(trimmedPhone.indexOf('+') + 1);
                if (!phoneNumber.matches("^\\d{6,}$")) {
                    log.warn("手机号验证失败：其他国家手机号格式不符合要求 - {}", trimmedPhone);
                    return false;
                }
            }
        } else {
            // 不带区号，按中国手机号验证
            if (!trimmedPhone.matches(SecurityConstants.PhonePolicy.PATTERN)) {
                log.warn("手机号验证失败：格式不符合要求 - {}", trimmedPhone);
                return false;
            }
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
     * 验证分页参数 (支持0基础分页)
     */
    public static boolean validatePageParams(Integer page, Integer pageSize, String paramName) {
        if (page == null || page < 0) {
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

    /**
     * 验证用户名（别名方法，与validateUsername相同）
     */
    public static boolean isValidUsername(String username) {
        return validateUsername(username);
    }

    /**
     * 验证密码（别名方法，与validatePassword相同）
     */
    public static boolean isValidPassword(String password) {
        return validatePassword(password);
    }

    /**
     * 验证角色名称
     */
    public static boolean isValidRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            log.warn("角色名称验证失败：角色名称为空");
            return false;
        }

        String trimmedRoleName = roleName.trim();
        // 角色名称应该以ROLE_开头，只包含大写字母、数字和下划线
        if (!trimmedRoleName.startsWith("ROLE_")) {
            log.warn("角色名称验证失败：必须以ROLE_开头 - {}", trimmedRoleName);
            return false;
        }

        if (trimmedRoleName.length() < 5 || trimmedRoleName.length() > 50) {
            log.warn("角色名称验证失败：长度不符合要求 - {}", trimmedRoleName.length());
            return false;
        }

        if (!trimmedRoleName.matches("^[A-Z0-9_]+$")) {
            log.warn("角色名称验证失败：格式不符合要求 - {}", trimmedRoleName);
            return false;
        }

        return true;
    }

    /**
     * 清理搜索关键词，防止SQL注入
     */
    public static String sanitizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "";
        }

        String sanitized = keyword.trim();
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        // 移除潜在的SQL注入字符
        sanitized = sanitized.replaceAll("[';\"\\-\\-\\*/\\\\]", "");

        return sanitized;
    }

    /**
     * 清理用户名
     */
    public static String sanitizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "";
        }

        String sanitized = username.trim();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        // 移除特殊字符，只保留字母、数字、下划线
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_]", "");

        return sanitized;
    }

    /**
     * 清理邮箱地址
     */
    public static String sanitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "";
        }

        String sanitized = email.trim().toLowerCase();
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        // 移除潜在的脚本注入字符
        sanitized = sanitized.replaceAll("['\"<>\\/&]", "");

        return sanitized;
    }

    /**
     * 清理手机号
     */
    public static String sanitizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }

        String sanitized = phone.trim();
        // 只保留数字、+号、-号
        sanitized = sanitized.replaceAll("[^0-9+\\-]", "");

        return sanitized;
    }

    /**
     * 清理角色名称
     */
    public static String sanitizeRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return "";
        }

        String sanitized = roleName.trim().toUpperCase();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        // 只保留字母、数字、下划线
        sanitized = sanitized.replaceAll("[^A-Z0-9_]", "");

        return sanitized;
    }

    /**
     * 清理权限名称
     */
    public static String sanitizePermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return "";
        }

        String sanitized = permission.trim();
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        // 移除潜在的注入字符
        sanitized = sanitized.replaceAll("[';\"\\-\\-\\*/\\\\]", "");

        return sanitized;
    }

    /**
     * 清理资源名称
     */
    public static String sanitizeResourceName(String resource) {
        if (resource == null || resource.trim().isEmpty()) {
            return "";
        }

        String sanitized = resource.trim().toLowerCase();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        // 只保留字母、数字、下划线
        sanitized = sanitized.replaceAll("[^a-z0-9_]", "");

        return sanitized;
    }

    /**
     * 清理动作名称
     */
    public static String sanitizeActionName(String action) {
        if (action == null || action.trim().isEmpty()) {
            return "";
        }

        String sanitized = action.trim().toLowerCase();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        // 只保留字母
        sanitized = sanitized.replaceAll("[^a-z]", "");

        return sanitized;
    }

    
    /**
     * 验证分页参数 (支持0基础分页)
     */
    public static boolean validatePaginationParams(Integer page, Integer pageSize, int maxPageSize) {
        if (page == null || page < 0) {
            log.warn("分页参数验证失败：页码必须大于等于0");
            return false;
        }

        if (pageSize == null || pageSize < 1 || pageSize > maxPageSize) {
            log.warn("分页参数验证失败：页面大小必须在1-{}之间", maxPageSize);
            return false;
        }

        return true;
    }

    /**
     * 验证日期格式（YYYY-MM-DD）
     * @param date 日期字符串
     * @return 是否为有效日期格式
     */
    public static boolean isValidDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return true; // 空值允许，由调用方处理
        }

        String trimmedDate = date.trim();
        String datePattern = "^\\d{4}-\\d{2}-\\d{2}$";

        if (!trimmedDate.matches(datePattern)) {
            log.warn("日期验证失败：格式不正确 - {}", trimmedDate);
            return false;
        }

        try {
            String[] parts = trimmedDate.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            // 基本日期验证
            if (year < 1900 || year > 2100) {
                log.warn("日期验证失败：年份超出范围 - {}", year);
                return false;
            }
            if (month < 1 || month > 12) {
                log.warn("日期验证失败：月份无效 - {}", month);
                return false;
            }
            if (day < 1 || day > 31) {
                log.warn("日期验证失败：日期无效 - {}", day);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("日期验证失败：解析错误 - {}", trimmedDate);
            return false;
        }
    }

    /**
     * 验证IP地址格式
     * @param ipAddress IP地址字符串
     * @return 是否为有效IP地址格式
     */
    public static boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return true; // 空值允许，由调用方处理
        }

        String trimmedIp = ipAddress.trim();

        // IPv4地址正则表达式
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        // IPv6地址正则表达式（简化版）
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";

        if (trimmedIp.matches(ipv4Pattern) || trimmedIp.matches(ipv6Pattern)) {
            return true;
        }

        log.warn("IP地址验证失败：格式不正确 - {}", trimmedIp);
        return false;
    }

    /**
     * 验证邮箱地址格式
     * @param email 邮箱地址字符串
     * @return 是否为有效邮箱地址格式
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true; // 空值允许，由调用方处理
        }

        String trimmedEmail = email.trim();

        // 邮箱地址正则表达式（RFC 5322标准简化版）
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

        if (trimmedEmail.matches(emailPattern)) {
            return true;
        }

        log.warn("邮箱地址验证失败：格式不正确 - {}", trimmedEmail);
        return false;
    }

    /**
     * 验证URL格式
     * @param url URL字符串
     * @return 是否为有效URL格式
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true; // 空值允许，由调用方处理
        }

        String trimmedUrl = url.trim();

        try {
            // URL格式验证正则表达式
            String urlPattern = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";

            if (trimmedUrl.matches(urlPattern)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("URL验证失败：解析错误 - {}", trimmedUrl);
        }

        log.warn("URL验证失败：格式不正确 - {}", trimmedUrl);
        return false;
    }
}
