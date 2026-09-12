package com.web.controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.UserPreferencesService;
import com.web.vo.user.preferences.NotificationPreferencesVo;
import com.web.vo.user.preferences.PrivacyPreferencesVo;
import com.web.vo.user.preferences.UserPreferencesVo;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/settings")
public class UserPreferencesController {
    private final UserPreferencesService preferences;

    public UserPreferencesController(UserPreferencesService preferences) {
        this.preferences = preferences;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserPreferencesVo>> get(@Userid Long userId) {
        return ResponseEntity.ok(ApiResponse.success(preferences.get(userId)));
    }

    @PutMapping("/privacy")
    public ResponseEntity<ApiResponse<PrivacyPreferencesVo>> savePrivacy(
            @Userid Long userId, @RequestBody @Valid PrivacyPreferencesVo privacy) {
        return ResponseEntity.ok(ApiResponse.success(preferences.savePrivacy(userId, privacy)));
    }

    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationPreferencesVo>> saveNotifications(
            @Userid Long userId, @RequestBody @Valid NotificationPreferencesVo notifications) {
        return ResponseEntity.ok(ApiResponse.success(preferences.saveNotifications(userId, notifications)));
    }
}
