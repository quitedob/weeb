package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 登录控制器
 * 处理第三方登录回调和用户信息获取
 */
@RestController
@RequestMapping("/oauth2")
@Slf4j
public class OAuth2Controller {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * OAuth2 登录成功回调
     */
    @GetMapping("/login/success")
    public ResponseEntity<ApiResponse<Map<String, Object>>> oauth2LoginSuccess(OAuth2AuthenticationToken authentication) {
        try {
            OAuth2User oauth2User = authentication.getPrincipal();
            String registrationId = authentication.getAuthorizedClientRegistrationId();

            log.info("OAuth2 login success for provider: {}", registrationId);

            // 从OAuth2用户信息中提取用户数据
            User user = extractUserFromOAuth2(oauth2User, registrationId);

            // 检查用户是否已存在
            User existingUser = authService.findByUserID(user.getId());
            if (existingUser == null) {
                // 新用户，注册
                authService.register(user);
                existingUser = user;
            }

            // 生成JWT token
            String token = jwtUtil.generateToken(existingUser.getId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("user", existingUser);

            return ResponseEntity.ok(ApiResponse.success("OAuth2登录成功", responseData));

        } catch (Exception e) {
            log.error("OAuth2登录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("OAuth2登录失败: " + e.getMessage()));
        }
    }

    /**
     * 从OAuth2用户信息中提取用户数据
     */
    private User extractUserFromOAuth2(OAuth2User oauth2User, String registrationId) {
        User user = new User();

        Map<String, Object> attributes = oauth2User.getAttributes();

        if ("google".equals(registrationId)) {
            // Google用户信息映射
            user.setUsername((String) attributes.get("email"));
            user.setUserEmail((String) attributes.get("email"));
            user.setNickname((String) attributes.get("name"));
            user.setAvatar((String) attributes.get("picture"));

        } else if ("microsoft".equals(registrationId)) {
            // Microsoft用户信息映射
            user.setUsername((String) attributes.get("preferred_username"));
            user.setUserEmail((String) attributes.get("email"));
            user.setNickname((String) attributes.get("name"));
            // Microsoft可能没有头像URL，需要额外处理
        }

        // 设置默认值
        if (user.getUsername() == null) {
            user.setUsername("oauth2_" + registrationId + "_" + attributes.get("sub"));
        }

        // 设置用户类型为第三方登录
        user.setUserType(1); // 假设1表示第三方登录用户

        return user;
    }

    /**
     * 获取OAuth2用户信息
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOAuth2User(OAuth2AuthenticationToken authentication) {
        try {
            OAuth2User oauth2User = authentication.getPrincipal();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("attributes", oauth2User.getAttributes());
            userInfo.put("authorities", oauth2User.getAuthorities());
            userInfo.put("name", oauth2User.getName());

            return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", userInfo));

        } catch (Exception e) {
            log.error("获取OAuth2用户信息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户信息失败: " + e.getMessage()));
        }
    }
}

