package com.web.vo.message;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息请求VO
 */
@Data
public class SendMessageVo {
    
    @NotNull(message = "目标用户ID不能为空")
    private Long targetId;
    
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;
    
    @NotNull(message = "消息内容不能为空")
    private Object content;
    
    /**
     * 群组ID（群聊时使用）
     */
    private Long groupId;
    
    /**
     * 是否显示时间
     */
    private Boolean showTime = false;
}