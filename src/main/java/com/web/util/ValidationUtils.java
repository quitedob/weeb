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
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".pdf", ".doc", ".docx", ".txt"};
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
}
