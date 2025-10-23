package com.web.vo.user;

import org.hibernate.validator.constraints.Length;

/**
 * 更新用户信息的VO
 * 用于接收前端传递的更新数据
 */
public class UpdateUserVo {

    @Length(max = 20, message = "用户名长度不能超过20个字符")
    private String username; // 用户名

    @Length(max = 255, message = "头像URL过长")
    private String avatar; // 头像URL

    @Length(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname; // 用户昵称

    @Length(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio; // 个人简介

    private String userEmail; // 用户邮箱

    @Length(max = 11, message = "手机号长度不能超过11个字符")
    private String phoneNumber; // 手机号码

    private Integer sex; // 性别：0为女，1为男

    // Getter 和 Setter 方法
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    // Add missing methods that controllers are calling
    public String getEmail() {
        return userEmail;
    }

    public String getPhone() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }
}