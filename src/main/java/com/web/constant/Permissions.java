package com.web.constant;

/**
 * 权限常量类
 * 定义系统中所有可用的权限
 */
public class Permissions {

    // 用户管理权限
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_READ = "USER_READ";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_BAN = "USER_BAN";
    public static final String USER_UNBAN = "USER_UNBAN";
    public static final String USER_RESET_PASSWORD = "USER_RESET_PASSWORD";
    public static final String USER_VIEW_PROFILE = "USER_VIEW_PROFILE";
    public static final String USER_EDIT_PROFILE = "USER_EDIT_PROFILE";

    // 文章管理权限
    public static final String ARTICLE_CREATE = "ARTICLE_CREATE";
    public static final String ARTICLE_READ = "ARTICLE_READ";
    public static final String ARTICLE_UPDATE = "ARTICLE_UPDATE";
    public static final String ARTICLE_DELETE = "ARTICLE_DELETE";
    public static final String ARTICLE_DELETE_OWN = "ARTICLE_DELETE_OWN";
    public static final String ARTICLE_DELETE_ANY = "ARTICLE_DELETE_ANY";
    public static final String ARTICLE_PUBLISH = "ARTICLE_PUBLISH";
    public static final String ARTICLE_UNPUBLISH = "ARTICLE_UNPUBLISH";
    public static final String ARTICLE_FEATURE = "ARTICLE_FEATURE";
    public static final String ARTICLE_LIKE = "ARTICLE_LIKE";
    public static final String ARTICLE_FAVORITE = "ARTICLE_FAVORITE";
    public static final String ARTICLE_COMMENT = "ARTICLE_COMMENT";

    // 评论管理权限
    public static final String COMMENT_CREATE = "COMMENT_CREATE";
    public static final String COMMENT_READ = "COMMENT_READ";
    public static final String COMMENT_UPDATE = "COMMENT_UPDATE";
    public static final String COMMENT_DELETE = "COMMENT_DELETE";
    public static final String COMMENT_DELETE_OWN = "COMMENT_DELETE_OWN";
    public static final String COMMENT_DELETE_ANY = "COMMENT_DELETE_ANY";
    public static final String COMMENT_MODERATE = "COMMENT_MODERATE";

    // 群组管理权限
    public static final String GROUP_CREATE = "GROUP_CREATE";
    public static final String GROUP_READ = "GROUP_READ";
    public static final String GROUP_UPDATE = "GROUP_UPDATE";
    public static final String GROUP_DELETE = "GROUP_DELETE";
    public static final String GROUP_DELETE_OWN = "GROUP_DELETE_OWN";
    public static final String GROUP_DELETE_ANY = "GROUP_DELETE_ANY";
    public static final String GROUP_JOIN = "GROUP_JOIN";
    public static final String GROUP_LEAVE = "GROUP_LEAVE";
    public static final String GROUP_MANAGE = "GROUP_MANAGE";
    public static final String GROUP_MODERATE = "GROUP_MODERATE";

    // 系统管理权限
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String SYSTEM_CONFIG = "SYSTEM_CONFIG";
    public static final String SYSTEM_LOG = "SYSTEM_LOG";
    public static final String SYSTEM_BACKUP = "SYSTEM_BACKUP";
    public static final String SYSTEM_RESTORE = "SYSTEM_RESTORE";
    public static final String SYSTEM_MONITOR = "SYSTEM_MONITOR";

    // 权限管理权限
    public static final String PERMISSION_CREATE = "PERMISSION_CREATE";
    public static final String PERMISSION_READ = "PERMISSION_READ";
    public static final String PERMISSION_UPDATE = "PERMISSION_UPDATE";
    public static final String PERMISSION_DELETE = "PERMISSION_DELETE";
    public static final String ROLE_CREATE = "ROLE_CREATE";
    public static final String ROLE_READ = "ROLE_READ";
    public static final String ROLE_UPDATE = "ROLE_UPDATE";
    public static final String ROLE_DELETE = "ROLE_DELETE";
    public static final String ROLE_ASSIGN = "ROLE_ASSIGN";
    public static final String ROLE_UNASSIGN = "ROLE_UNASSIGN";

    // 搜索权限
    public static final String SEARCH_USER = "SEARCH_USER";
    public static final String SEARCH_ARTICLE = "SEARCH_ARTICLE";
    public static final String SEARCH_GROUP = "SEARCH_GROUP";
    public static final String SEARCH_ADVANCED = "SEARCH_ADVANCED";

    // 文件管理权限
    public static final String FILE_UPLOAD = "FILE_UPLOAD";
    public static final String FILE_DOWNLOAD = "FILE_DOWNLOAD";
    public static final String FILE_DELETE = "FILE_DELETE";
    public static final String FILE_DELETE_OWN = "FILE_DELETE_OWN";
    public static final String FILE_DELETE_ANY = "FILE_DELETE_ANY";
    public static final String FILE_MANAGE = "FILE_MANAGE";

    // 消息权限
    public static final String MESSAGE_SEND = "MESSAGE_SEND";
    public static final String MESSAGE_READ = "MESSAGE_READ";
    public static final String MESSAGE_DELETE = "MESSAGE_DELETE";
    public static final String MESSAGE_DELETE_OWN = "MESSAGE_DELETE_OWN";
    public static final String MESSAGE_DELETE_ANY = "MESSAGE_DELETE_ANY";
    public static final String MESSAGE_RECALL = "MESSAGE_RECALL";
    public static final String MESSAGE_RECALL_OWN = "MESSAGE_RECALL_OWN";
    public static final String MESSAGE_RECALL_ANY = "MESSAGE_RECALL_ANY";

    // 通知权限
    public static final String NOTIFICATION_SEND = "NOTIFICATION_SEND";
    public static final String NOTIFICATION_READ = "NOTIFICATION_READ";
    public static final String NOTIFICATION_DELETE = "NOTIFICATION_DELETE";
    public static final String NOTIFICATION_MANAGE = "NOTIFICATION_MANAGE";

    // 安全权限
    public static final String SECURITY_AUDIT = "SECURITY_AUDIT";
    public static final String SECURITY_CONFIG = "SECURITY_CONFIG";
    public static final String SECURITY_SESSION = "SECURITY_SESSION";
    public static final String SECURITY_SESSION_KILL = "SECURITY_SESSION_KILL";
    public static final String SECURITY_2FA = "SECURITY_2FA";
    public static final String SECURITY_PASSWORD = "SECURITY_PASSWORD";

    // API权限
    public static final String API_ACCESS = "API_ACCESS";
    public static final String API_ADMIN = "API_ADMIN";
    public static final String API_WEBHOOK = "API_WEBHOOK";

    private Permissions() {
        // 私有构造器，防止实例化
    }

    /**
     * 获取所有权限常量
     * @return 权限常量数组
     */
    public static String[] getAllPermissions() {
        return new String[] {
            // 用户管理
            USER_CREATE, USER_READ, USER_UPDATE, USER_DELETE, USER_BAN, USER_UNBAN,
            USER_RESET_PASSWORD, USER_VIEW_PROFILE, USER_EDIT_PROFILE,

            // 文章管理
            ARTICLE_CREATE, ARTICLE_READ, ARTICLE_UPDATE, ARTICLE_DELETE,
            ARTICLE_DELETE_OWN, ARTICLE_DELETE_ANY, ARTICLE_PUBLISH, ARTICLE_UNPUBLISH,
            ARTICLE_FEATURE, ARTICLE_LIKE, ARTICLE_FAVORITE, ARTICLE_COMMENT,

            // 评论管理
            COMMENT_CREATE, COMMENT_READ, COMMENT_UPDATE, COMMENT_DELETE,
            COMMENT_DELETE_OWN, COMMENT_DELETE_ANY, COMMENT_MODERATE,

            // 群组管理
            GROUP_CREATE, GROUP_READ, GROUP_UPDATE, GROUP_DELETE,
            GROUP_DELETE_OWN, GROUP_DELETE_ANY, GROUP_JOIN, GROUP_LEAVE,
            GROUP_MANAGE, GROUP_MODERATE,

            // 系统管理
            SYSTEM_ADMIN, SYSTEM_CONFIG, SYSTEM_LOG, SYSTEM_BACKUP, SYSTEM_RESTORE, SYSTEM_MONITOR,

            // 权限管理
            PERMISSION_CREATE, PERMISSION_READ, PERMISSION_UPDATE, PERMISSION_DELETE,
            ROLE_CREATE, ROLE_READ, ROLE_UPDATE, ROLE_DELETE, ROLE_ASSIGN, ROLE_UNASSIGN,

            // 搜索
            SEARCH_USER, SEARCH_ARTICLE, SEARCH_GROUP, SEARCH_ADVANCED,

            // 文件管理
            FILE_UPLOAD, FILE_DOWNLOAD, FILE_DELETE, FILE_DELETE_OWN, FILE_DELETE_ANY, FILE_MANAGE,

            // 消息
            MESSAGE_SEND, MESSAGE_READ, MESSAGE_DELETE, MESSAGE_DELETE_OWN, MESSAGE_DELETE_ANY,
            MESSAGE_RECALL, MESSAGE_RECALL_OWN, MESSAGE_RECALL_ANY,

            // 通知
            NOTIFICATION_SEND, NOTIFICATION_READ, NOTIFICATION_DELETE, NOTIFICATION_MANAGE,

            // 安全
            SECURITY_AUDIT, SECURITY_CONFIG, SECURITY_SESSION, SECURITY_SESSION_KILL,
            SECURITY_2FA, SECURITY_PASSWORD,

            // API
            API_ACCESS, API_ADMIN, API_WEBHOOK
        };
    }
}
