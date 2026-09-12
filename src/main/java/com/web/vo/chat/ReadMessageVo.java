package com.web.vo.chat;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ReadMessageVo {
    @PositiveOrZero
    private Long lastReadMessageId;
}
