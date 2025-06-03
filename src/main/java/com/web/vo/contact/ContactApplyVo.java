package com.web.vo.contact;

import lombok.Data;
// Using jakarta.validation.constraints.*
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class ContactApplyVo {
    @NotNull
    private Long friendId; // 目标好友的用户ID (Changed to Long)

    @Size(max = 50)
    private String remarks; // 申请附言
}
