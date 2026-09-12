package com.web.service;

import com.web.common.ApiResponse;
import com.web.exception.WeebException;
import com.web.mapper.UserPreferencesMapper;
import com.web.model.UserPreferences;
import com.web.vo.user.preferences.NotificationPreferencesVo;
import com.web.vo.user.preferences.PrivacyPreferencesVo;
import com.web.vo.user.preferences.UserPreferencesVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserPreferencesService {
    private final UserPreferencesMapper mapper;

    public UserPreferencesService(UserPreferencesMapper mapper) {
        this.mapper = mapper;
    }

    public UserPreferencesVo get(Long userId) {
        requireUserId(userId);
        UserPreferences preferences = mapper.findByUserId(userId);
        return UserPreferencesVo.from(preferences == null ? new UserPreferences() : preferences);
    }

    @Transactional
    public PrivacyPreferencesVo savePrivacy(Long userId, PrivacyPreferencesVo privacy) {
        requireUserId(userId);
        if (privacy == null) throw invalidSection();
        requireBooleans(privacy.getOnlineVisible(), privacy.getAllowMessages(), privacy.getShowFollows());
        mapper.upsertPrivacy(userId, privacy);
        return get(userId).getPrivacy();
    }

    @Transactional
    public NotificationPreferencesVo saveNotifications(Long userId, NotificationPreferencesVo notifications) {
        requireUserId(userId);
        if (notifications == null) throw invalidSection();
        requireBooleans(notifications.getNewMessages(), notifications.getFollows(), notifications.getLikes(),
                notifications.getComments(), notifications.getGroupInvites());
        mapper.upsertNotifications(userId, notifications);
        return get(userId).getNotifications();
    }

    public boolean canReceiveMessages(Long userId) {
        return get(userId).getPrivacy().getAllowMessages();
    }

    public boolean isOnlineVisible(Long userId) {
        return get(userId).getPrivacy().getOnlineVisible();
    }

    public boolean isFollowingVisible(Long userId) {
        return get(userId).getPrivacy().getShowFollows();
    }

    public boolean isNotificationEnabled(Long userId, String type) {
        NotificationPreferencesVo preferences = get(userId).getNotifications();
        String normalized = type == null ? "" : type.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LIKE", "ARTICLE_LIKE" -> preferences.getLikes();
            case "FOLLOW", "NEW_FOLLOWER" -> preferences.getFollows();
            case "COMMENT", "COMMENT_MENTION" -> preferences.getComments();
            case "MESSAGE", "NEW_MESSAGE" -> preferences.getNewMessages();
            case "GROUP_INVITE", "GROUP_INVITATION", "GROUP_APPLICATION",
                    "GROUP_APPLICATION_APPROVED", "GROUP_APPLICATION_REJECTED" -> preferences.getGroupInvites();
            // Contact synchronization and system notices have no user-facing toggle.
            default -> true;
        };
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WeebException(ApiResponse.ErrorCode.UNAUTHORIZED, "Authenticated user required");
        }
    }

    private static void requireBooleans(Boolean... values) {
        for (Boolean value : values) {
            if (value == null) throw invalidSection();
        }
    }

    private static WeebException invalidSection() {
        return new WeebException(ApiResponse.ErrorCode.PARAM_ERROR, "Every preference in the section is required");
    }
}
