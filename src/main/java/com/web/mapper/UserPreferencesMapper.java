package com.web.mapper;

import com.web.model.UserPreferences;
import com.web.vo.user.preferences.NotificationPreferencesVo;
import com.web.vo.user.preferences.PrivacyPreferencesVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserPreferencesMapper {
    @Select("""
            SELECT user_id AS userId, online_visible AS onlineVisible, allow_messages AS allowMessages,
                   show_follows AS showFollows, new_messages AS newMessages, follows, likes, comments,
                   group_invites AS groupInvites, updated_at AS updatedAt
            FROM user_preferences WHERE user_id = #{userId}
            """)
    UserPreferences findByUserId(@Param("userId") Long userId);

    // Each statement updates only its own section, so concurrent section saves cannot overwrite each other.
    @Insert("""
            INSERT INTO user_preferences (user_id, online_visible, allow_messages, show_follows)
            VALUES (#{userId}, #{privacy.onlineVisible}, #{privacy.allowMessages}, #{privacy.showFollows})
            ON DUPLICATE KEY UPDATE online_visible = #{privacy.onlineVisible},
                allow_messages = #{privacy.allowMessages}, show_follows = #{privacy.showFollows},
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertPrivacy(@Param("userId") Long userId, @Param("privacy") PrivacyPreferencesVo privacy);

    @Insert("""
            INSERT INTO user_preferences (user_id, new_messages, follows, likes, comments, group_invites)
            VALUES (#{userId}, #{notifications.newMessages}, #{notifications.follows}, #{notifications.likes},
                    #{notifications.comments}, #{notifications.groupInvites})
            ON DUPLICATE KEY UPDATE new_messages = #{notifications.newMessages}, follows = #{notifications.follows},
                likes = #{notifications.likes}, comments = #{notifications.comments},
                group_invites = #{notifications.groupInvites}, updated_at = CURRENT_TIMESTAMP
            """)
    int upsertNotifications(@Param("userId") Long userId,
                            @Param("notifications") NotificationPreferencesVo notifications);
}
