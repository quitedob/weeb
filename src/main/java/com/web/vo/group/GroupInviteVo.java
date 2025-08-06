package com.web.vo.group;

import lombok.Data;
// Using jakarta.validation.constraints.*
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class GroupInviteVo {
    @NotNull
    private Long groupId; // 群组ID (Changed to Long, assuming Group.id is Long)

    @NotNull
    private List<Long> memberIds; // 被邀请的成员ID列表 (Changed to Long)
}
