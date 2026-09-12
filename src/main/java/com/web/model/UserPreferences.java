package com.web.model;

import lombok.Data;

import java.time.LocalDateTime;

/** Persisted account preferences. A missing row has the same defaults as a new row. */
@Data
public class UserPreferences {
    private Long userId;
    private boolean onlineVisible = true;
    private boolean allowMessages = true;
    private boolean showFollows = true;
    private boolean newMessages = true;
    private boolean follows = true;
    private boolean likes = true;
    private boolean comments = true;
    private boolean groupInvites = true;
    private LocalDateTime updatedAt;
}
