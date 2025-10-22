package com.web.vo.message;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 撤回消息的请求视图对象 (VO)
 */
@Data
public class RecallRequestVo {
    @NotNull(message = "消息ID不能为空")
    private Long msgId; // 要撤回的消息ID
}
