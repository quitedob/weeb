package com.web.vo.message;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 获取聊天记录的请求视图对象 (VO)
 */
@Data
public class RecordRequestVo {
    @NotNull(message = "目标ID不能为空")
    private Long targetId; // 目标聊天对象的ID

    private int index; // 分页查询的起始索引

    @Max(value = 100, message = "查询条数不能超过100条")
    private int num; // 查询条数
}
