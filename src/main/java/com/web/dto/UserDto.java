package com.web.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户数据传输对象 (UserDto)。
 * 用于封装用户相关信息，便于在系统中传递和处理。
 */
@Data // Lombok注解，自动生成Getter、Setter、toString等方法
public class UserDto {

    /**
     * 用户ID。
     * 唯一标识用户的标识符，例如数据库中的主键ID或UUID。
     */
    private Long id;

    /**
     * 用户名称。
     * 表示用户的昵称或真实姓名，用于显示或交互。
     */
    private String name;

    /**
     * 用户头像。
     * 存储用户头像的URL地址或路径，便于前端渲染用户形象。
     */
    private String avatar;

    /**
     * 用户类型。
     * 表示用户的类型，例如普通用户、管理员、访客等。
     * 可以根据类型决定用户的权限和功能范围。
     */
    private String type;

    /**
     * 用户徽章列表。
     * 存储用户拥有的徽章信息，例如荣誉、等级标识等。
     * 使用列表形式便于扩展多个徽章。
     */
    private List<String> badge;

    /**
     * 用户IP归属地。
     * 表示用户当前IP地址的归属地信息，例如城市、国家等。
     * 可用于安全审计或显示地理位置。
     */
    private String ipOwnership;
}
