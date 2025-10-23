package com.web.service.impl;

import com.web.model.Permission;
import com.web.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统权限初始化器
 * 在应用启动时自动创建所有预定义的系统权限
 */
@Component
@Slf4j
@Order(2) // 确保在DatabaseInitializer之后执行
public class SystemPermissionInitializer implements CommandLineRunner {

    private final PermissionService permissionService;

    @Autowired
    public SystemPermissionInitializer(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化系统权限...");

        Map<String, Object> result = permissionService.initializeSystemPermissions();

        int createdCount = (Integer) result.get("createdCount");
        int totalCount = (Integer) result.get("totalCount");

        log.info("系统权限初始化完成: 新创建 {} 个权限，总共 {} 个权限", createdCount, totalCount);
    }

    /**
     * 获取所有预定义的系统权限
     * @return 系统权限列表
     */
    public static List<Permission> getSystemPermissions() {
        List<Permission> permissions = new ArrayList<>();

        // 用户管理权限
        permissions.addAll(getUserManagementPermissions());

        // 文章管理权限
        permissions.addAll(getArticleManagementPermissions());

        // 消息管理权限
        permissions.addAll(getMessageManagementPermissions());

        // 群组管理权限
        permissions.addAll(getGroupManagementPermissions());

        // 系统管理权限
        permissions.addAll(getSystemManagementPermissions());

        return permissions;
    }

    /**
     * 用户管理权限
     */
    private static List<Permission> getUserManagementPermissions() {
        List<Permission> permissions = new ArrayList<>();

        // 基础用户权限
        permissions.add(new Permission("USER_CREATE_OWN", "创建用户", "user", "create", "OWN", 1, 0, "用户管理"));
        permissions.add(new Permission("USER_READ_OWN", "查看自己的用户信息", "user", "read", "OWN", 1, 0, "用户管理"));
        permissions.add(new Permission("USER_UPDATE_OWN", "更新自己的用户信息", "user", "update", "OWN", 1, 0, "用户管理"));
        permissions.add(new Permission("USER_DELETE_OWN", "删除自己的用户", "user", "delete", "OWN", 1, 0, "用户管理"));

        // 管理员用户权限
        permissions.add(new Permission("USER_CREATE_ANY", "创建任意用户", "user", "create", "ANY", 1, 0, "用户管理"));
        permissions.add(new Permission("USER_READ_ANY", "查看任意用户信息", "user", "read", "ANY", 1, 0, "用户管理"));
        permissions.add(new Permission("USER_UPDATE_ANY", "更新任意用户信息", "user", "update", "ANY", 1, 0, "用户管理"));
        permissions.add(new Permission("USER_DELETE_ANY", "删除任意用户", "user", "delete", "ANY", 1, 0, "用户管理"));
        permissions.add(new Permission("USER_BAN_ANY", "封禁/解封用户", "user", "ban", "ANY", 1, 0, "用户管理"));
        permissions.add(new Permission("USER_RESET_PASSWORD_ANY", "重置任意用户密码", "user", "reset_password", "ANY", 1, 0, "用户管理"));

        // 用户关系权限
        permissions.add(new Permission("USER_FOLLOW_OWN", "关注用户", "user", "follow", "OWN", 1, 1, "用户管理"));

        return permissions;
    }

    /**
     * 文章管理权限
     */
    private static List<Permission> getArticleManagementPermissions() {
        List<Permission> permissions = new ArrayList<>();

        // 基础文章权限
        permissions.add(new Permission("ARTICLE_CREATE_OWN", "创建文章", "article", "create", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("ARTICLE_READ_OWN", "查看自己的文章", "article", "read", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("ARTICLE_UPDATE_OWN", "更新自己的文章", "article", "update", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("ARTICLE_DELETE_OWN", "删除自己的文章", "article", "delete", "OWN", 1, 1, "内容管理"));

        // 管理员文章权限
        permissions.add(new Permission("ARTICLE_READ_ANY", "查看任意文章", "article", "read", "ANY", 1, 0, "内容管理"));
        permissions.add(new Permission("ARTICLE_UPDATE_ANY", "更新任意文章", "article", "update", "ANY", 1, 0, "内容管理"));
        permissions.add(new Permission("ARTICLE_DELETE_ANY", "删除任意文章", "article", "delete", "ANY", 1, 0, "内容管理"));
        permissions.add(new Permission("ARTICLE_PUBLISH_ANY", "发布/下架任意文章", "article", "publish", "ANY", 1, 0, "内容管理"));
        permissions.add(new Permission("ARTICLE_FEATURE_ANY", "推荐/取消推荐任意文章", "article", "feature", "ANY", 1, 0, "内容管理"));

        // 互动权限
        permissions.add(new Permission("ARTICLE_LIKE_OWN", "点赞文章", "article", "like", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("ARTICLE_FAVORITE_OWN", "收藏文章", "article", "favorite", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("ARTICLE_COMMENT_OWN", "评论文章", "article", "comment", "OWN", 1, 1, "内容管理"));

        // 评论管理权限
        permissions.add(new Permission("ARTICLE_COMMENT_DELETE_ANY", "删除任意评论", "article", "comment_delete", "ANY", 1, 0, "内容管理"));

        // 文章版本权限
        permissions.add(new Permission("ARTICLE_VERSION_READ_OWN", "查看文章版本", "article", "version_read", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("ARTICLE_VERSION_CREATE_OWN", "创建文章版本", "article", "version_create", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("ARTICLE_VERSION_ROLLBACK_OWN", "回滚文章版本", "article", "version_rollback", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("ARTICLE_VERSION_DELETE_ANY", "删除文章版本", "article", "version_delete", "ANY", 1, 0, "内容管理"));

        return permissions;
    }

    /**
     * 消息管理权限
     */
    private static List<Permission> getMessageManagementPermissions() {
        List<Permission> permissions = new ArrayList<>();

        // 基础消息权限
        permissions.add(new Permission("MESSAGE_CREATE_OWN", "发送消息", "message", "create", "OWN", 1, 1, "消息管理"));
        permissions.add(new Permission("MESSAGE_READ_OWN", "查看自己的消息", "message", "read", "OWN", 1, 1, "消息管理"));
        permissions.add(new Permission("MESSAGE_UPDATE_OWN", "更新自己的消息", "message", "update", "OWN", 1, 1, "消息管理"));
        permissions.add(new Permission("MESSAGE_DELETE_OWN", "删除自己的消息", "message", "delete", "OWN", 1, 1, "消息管理"));

        // 管理员消息权限
        permissions.add(new Permission("MESSAGE_READ_ANY", "查看任意消息", "message", "read", "ANY", 1, 0, "消息管理"));
        permissions.add(new Permission("MESSAGE_DELETE_ANY", "删除任意消息", "message", "delete", "ANY", 1, 0, "消息管理"));
        permissions.add(new Permission("MESSAGE_SEARCH_ANY", "搜索消息", "message", "search", "ANY", 1, 0, "消息管理"));

        // 消息线索权限
        permissions.add(new Permission("MESSAGE_THREAD_CREATE_OWN", "创建消息线索", "message_thread", "create", "OWN", 1, 1, "消息管理"));
        permissions.add(new Permission("MESSAGE_THREAD_READ_OWN", "查看消息线索", "message_thread", "read", "OWN", 1, 1, "消息管理"));
        permissions.add(new Permission("MESSAGE_THREAD_REPLY_OWN", "回复消息线索", "message_thread", "reply", "OWN", 1, 1, "消息管理"));
        permissions.add(new Permission("MESSAGE_THREAD_MANAGE_OWN", "管理自己的消息线索", "message_thread", "manage", "OWN", 1, 1, "消息管理"));

        // 链接预览权限
        permissions.add(new Permission("LINK_PREVIEW_CREATE_OWN", "创建链接预览", "link_preview", "create", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("LINK_PREVIEW_READ_OWN", "查看链接预览", "link_preview", "read", "OWN", 1, 1, "内容管理"));
        permissions.add(new Permission("LINK_PREVIEW_MANAGE_OWN", "管理自己的链接预览", "link_preview", "manage", "OWN", 1, 1, "内容管理"));

        return permissions;
    }

    /**
     * 群组管理权限
     */
    private static List<Permission> getGroupManagementPermissions() {
        List<Permission> permissions = new ArrayList<>();

        // 基础群组权限
        permissions.add(new Permission("GROUP_CREATE_OWN", "创建群组", "group", "create", "OWN", 1, 1, "群组管理"));
        permissions.add(new Permission("GROUP_READ_OWN", "查看自己的群组", "group", "read", "OWN", 1, 1, "群组管理"));
        permissions.add(new Permission("GROUP_UPDATE_OWN", "更新自己的群组", "group", "update", "OWN", 1, 1, "群组管理"));
        permissions.add(new Permission("GROUP_DELETE_OWN", "删除自己的群组", "group", "delete", "OWN", 1, 1, "群组管理"));

        // 管理员群组权限
        permissions.add(new Permission("GROUP_READ_ANY", "查看任意群组", "group", "read", "ANY", 1, 0, "群组管理"));
        permissions.add(new Permission("GROUP_UPDATE_ANY", "更新任意群组", "group", "update", "ANY", 1, 0, "群组管理"));
        permissions.add(new Permission("GROUP_DELETE_ANY", "删除任意群组", "group", "delete", "ANY", 1, 0, "群组管理"));
        permissions.add(new Permission("GROUP_MANAGE_MEMBERS_ANY", "管理任意群组成员", "group", "manage_members", "ANY", 1, 0, "群组管理"));

        // 群组成员权限
        permissions.add(new Permission("GROUP_JOIN_OWN", "加入群组", "group", "join", "OWN", 1, 1, "群组管理"));
        permissions.add(new Permission("GROUP_LEAVE_OWN", "退出群组", "group", "leave", "OWN", 1, 1, "群组管理"));
        permissions.add(new Permission("GROUP_INVITE_OWN", "邀请用户加入群组", "group", "invite", "OWN", 1, 1, "群组管理"));

        return permissions;
    }

    /**
     * 系统管理权限
     */
    private static List<Permission> getSystemManagementPermissions() {
        List<Permission> permissions = new ArrayList<>();

        // 权限管理
        permissions.add(new Permission("PERMISSION_READ_ANY", "查看权限列表", "permission", "read", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("PERMISSION_CREATE_ANY", "创建权限", "permission", "create", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("PERMISSION_UPDATE_ANY", "更新权限", "permission", "update", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("PERMISSION_DELETE_ANY", "删除权限", "permission", "delete", "ANY", 1, 0, "系统管理"));

        // 角色管理
        permissions.add(new Permission("ROLE_READ_ANY", "查看角色列表", "role", "read", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("ROLE_CREATE_ANY", "创建角色", "role", "create", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("ROLE_UPDATE_ANY", "更新角色", "role", "update", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("ROLE_DELETE_ANY", "删除角色", "role", "delete", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("ROLE_ASSIGN_ANY", "分配角色给用户", "role", "assign", "ANY", 1, 0, "系统管理"));

        // 系统配置
        permissions.add(new Permission("SYSTEM_CONFIG_READ", "查看系统配置", "system", "config_read", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("SYSTEM_CONFIG_UPDATE", "更新系统配置", "system", "config_update", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("SYSTEM_STATS_READ", "查看系统统计", "system", "stats_read", "ANY", 1, 0, "系统管理"));
        permissions.add(new Permission("SYSTEM_LOG_READ", "查看系统日志", "system", "log_read", "ANY", 1, 0, "系统管理"));

        // 文件管理
        permissions.add(new Permission("FILE_UPLOAD_OWN", "上传文件", "file", "upload", "OWN", 1, 1, "文件管理"));
        permissions.add(new Permission("FILE_READ_OWN", "查看自己的文件", "file", "read", "OWN", 1, 1, "文件管理"));
        permissions.add(new Permission("FILE_DELETE_OWN", "删除自己的文件", "file", "delete", "OWN", 1, 1, "文件管理"));
        permissions.add(new Permission("FILE_READ_ANY", "查看任意文件", "file", "read", "ANY", 1, 0, "文件管理"));
        permissions.add(new Permission("FILE_DELETE_ANY", "删除任意文件", "file", "delete", "ANY", 1, 0, "文件管理"));

        // 内容审核
        permissions.add(new Permission("CONTENT_MODERATE_ANY", "审核内容", "content", "moderate", "ANY", 1, 0, "内容管理"));
        permissions.add(new Permission("CONTENT_REPORT_READ", "查看举报内容", "content", "report_read", "ANY", 1, 0, "内容管理"));
        permissions.add(new Permission("CONTENT_REPORT_PROCESS", "处理举报", "content", "report_process", "ANY", 1, 0, "内容管理"));

        // AI服务权限
        permissions.add(new Permission("AI_CHAT_OWN", "AI对话", "ai", "chat", "OWN", 1, 1, "AI服务"));
        permissions.add(new Permission("AI_MANAGE_ANY", "管理AI服务", "ai", "manage", "ANY", 1, 0, "AI服务"));

        // SSH终端权限
        permissions.add(new Permission("SSH_EXECUTE_OWN", "执行SSH命令", "ssh", "execute", "OWN", 1, 0, "系统管理"));
        permissions.add(new Permission("SSH_MANAGE_ANY", "管理SSH服务", "ssh", "manage", "ANY", 1, 0, "系统管理"));

        // 认证权限
        permissions.add(new Permission("AUTH_REGISTER_OWN", "用户注册", "auth", "register", "OWN", 1, 1, "认证管理"));
        permissions.add(new Permission("AUTH_LOGIN_OWN", "用户登录", "auth", "login", "OWN", 1, 1, "认证管理"));
        permissions.add(new Permission("AUTH_LOGOUT_OWN", "用户登出", "auth", "logout", "OWN", 1, 1, "认证管理"));
        permissions.add(new Permission("AUTH_PASSWORD_CHANGE_OWN", "修改密码", "auth", "password_change", "OWN", 1, 1, "认证管理"));

        return permissions;
    }
}