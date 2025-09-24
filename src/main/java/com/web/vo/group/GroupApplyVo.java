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
    
    @Size(max = 200, message = "申请理由不能超过200个字符")
    private String reason; // 申请理由
}