package com.web.vo.group;

import lombok.Data;
// Using jakarta.validation.constraints.*
import jakarta.validation.constraints.NotNull;

@Data
public class GroupKickVo {
    @NotNull
    private Long groupId; // 群组ID (Changed to Long)

    @NotNull
    private Long kickedUserId; // 被踢出的用户ID (Changed to Long)

    private String reason; // 踢出原因（可选）
}
