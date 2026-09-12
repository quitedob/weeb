package com.web.vo.user.preferences;

import com.web.model.UserPreferences;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** API response deliberately contains no actor ID supplied by a client. */
@Getter
@AllArgsConstructor
public class UserPreferencesVo {
    private final PrivacyPreferencesVo privacy;
    private final NotificationPreferencesVo notifications;

    public static UserPreferencesVo from(UserPreferences preferences) {
        return new UserPreferencesVo(
                new PrivacyPreferencesVo(preferences.isOnlineVisible(), preferences.isAllowMessages(),
                        preferences.isShowFollows()),
                new NotificationPreferencesVo(preferences.isNewMessages(), preferences.isFollows(),
                        preferences.isLikes(), preferences.isComments(), preferences.isGroupInvites()));
    }
}
