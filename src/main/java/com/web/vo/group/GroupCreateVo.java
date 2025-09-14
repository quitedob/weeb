package com.web.vo.group;

import lombok.Data;
// Using jakarta.validation.constraints.*
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class GroupCreateVo {
    @NotEmpty
    @Size(min = 1, max = 20)
    private String groupName; // 群名称

    @Size(max = 50) // 初始成员可选
    private List<Long> initialMemberIds; // 初始成员ID列表 (Changed to Long)
}
