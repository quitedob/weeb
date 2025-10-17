package com.web.service;

import com.web.model.User;
import java.util.Map;

/**
 * 密码重置服务接口
 * 提供安全的密码重置功能
 */
public interface PasswordResetService {

    /**
     * 发送密码重置链接到用户邮箱
     * @param email 用户邮箱
     * @return 操作结果
     */
    Map<String, Object> sendPasswordResetLink(String email);

    /**
     * 验证重置令牌是否有效
     * @param token 重置令牌
     * @return 验证结果
     */
    Map<String, Object> validateResetToken(String token);

    /**
     * 执行密码重置
     * @param token 重置令牌
     * @param newPassword 新密码
     * @param confirmPassword 确认密码
     * @return 操作结果
     */
    Map<String, Object> resetPassword(String token, String newPassword, String confirmPassword);

    /**
     * 使重置令牌失效
     * @param token 重置令牌
     */
    void invalidateResetToken(String token);
}