package com.web.vo.video;

import lombok.Data;

@Data
public class VideoInviteVo {
    private String userId;
    private boolean isOnlyAudio;

    public void setIsOnlyAudio(boolean isOnlyAudio) {
        this.isOnlyAudio = isOnlyAudio;
    }
}
