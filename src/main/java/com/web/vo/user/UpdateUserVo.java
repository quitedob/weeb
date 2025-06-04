package com.web.vo.user;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 更新用户信息的VO
 * 用于接收前端传递的更新数据
 */
@Data
public class UpdateUserVo {

    @Length(max = 20, message = "用户名长度不能超过20个字符")
    private String username; // 用户名

    @Length(max = 255, message = "头像URL过长")
    private String avatar; // 头像URL
}
