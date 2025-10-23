package com.web.vo.group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 群组申请加入VO
 */
@Data
public class GroupApplyVo {

    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    private Long userId; // 申请用户ID

    @Size(max = 200, message = "申请理由不能超过200个字符")
    private String reason; // 申请理由

    @Size(max = 500, message = "申请消息不能超过500个字符")
    private String message; // 申请消息
}