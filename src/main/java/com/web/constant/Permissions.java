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

    // 用户自有权限（普通用户基础权限）
    public static final String USER_READ_OWN = "USER_READ_OWN";
    public static final String USER_UPDATE_OWN = "USER_UPDATE_OWN";
    public static final String USER_EDIT_PROFILE_OWN = "USER_EDIT_PROFILE_OWN";
    public static final String USER_SETTINGS_OWN = "USER_SETTINGS_OWN";

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

    // 文章自有权限（普通用户基础权限）
    public static final String ARTICLE_CREATE_OWN = "ARTICLE_CREATE_OWN";
    public static final String ARTICLE_READ_OWN = "ARTICLE_READ_OWN";
    public static final String ARTICLE_UPDATE_OWN = "ARTICLE_UPDATE_OWN";
    public static final String ARTICLE_DELETE_OWN_USER = "ARTICLE_DELETE_OWN_USER";
    public static final String ARTICLE_FAVORITE_OWN = "ARTICLE_FAVORITE_OWN";
    public static final String ARTICLE_COMMENT_OWN = "ARTICLE_COMMENT_OWN";

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

    // 群组自有权限（普通用户基础权限）
    public static final String GROUP_CREATE_OWN = "GROUP_CREATE_OWN";
    public static final String GROUP_READ_OWN = "GROUP_READ_OWN";
    public static final String GROUP_UPDATE_OWN = "GROUP_UPDATE_OWN";
    public static final String GROUP_JOIN_OWN = "GROUP_JOIN_OWN";
    public static final String GROUP_LEAVE_OWN = "GROUP_LEAVE_OWN";

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

    // 搜索基础权限（普通用户）
    public static final String SEARCH_BASIC = "SEARCH_BASIC";
    public static final String SEARCH_USER_BASIC = "SEARCH_USER_BASIC";
    public static final String SEARCH_CONTENT_BASIC = "SEARCH_CONTENT_BASIC";

    // 消息权限
    public static final String MESSAGE_SEND = "MESSAGE_SEND";
    public static final String MESSAGE_READ = "MESSAGE_READ";
    public static final String MESSAGE_DELETE = "MESSAGE_DELETE";
    public static final String MESSAGE_DELETE_OWN = "MESSAGE_DELETE_OWN";
    public static final String MESSAGE_DELETE_ANY = "MESSAGE_DELETE_ANY";
    public static final String MESSAGE_RECALL = "MESSAGE_RECALL";
    public static final String MESSAGE_RECALL_OWN = "MESSAGE_RECALL_OWN";
    public static final String MESSAGE_RECALL_ANY = "MESSAGE_RECALL_ANY";

    // 消息基础权限（普通用户）
    public static final String MESSAGE_CREATE_OWN = "MESSAGE_CREATE_OWN";
    public static final String MESSAGE_READ_OWN = "MESSAGE_READ_OWN";
    public static final String MESSAGE_UPDATE_OWN = "MESSAGE_UPDATE_OWN";
    public static final String MESSAGE_DELETE_OWN_USER = "MESSAGE_DELETE_OWN_USER";

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
    public static final String SECURITY_PASSWORD = "SECURITY_PASSWORD";

    // API权限
    public static final String API_ACCESS = "API_ACCESS";
    public static final String API_ADMIN = "API_ADMIN";
    public static final String API_WEBHOOK = "API_WEBHOOK";

    // 联系人权限（好友功能）
    public static final String CONTACT_CREATE = "CONTACT_CREATE";
    public static final String CONTACT_READ = "CONTACT_READ";
    public static final String CONTACT_UPDATE = "CONTACT_UPDATE";
    public static final String CONTACT_DELETE = "CONTACT_DELETE";

    // 联系人自有权限（普通用户）
    public static final String CONTACT_CREATE_OWN = "CONTACT_CREATE_OWN";
    public static final String CONTACT_READ_OWN = "CONTACT_READ_OWN";
    public static final String CONTACT_UPDATE_OWN = "CONTACT_UPDATE_OWN";
    public static final String CONTACT_DELETE_OWN = "CONTACT_DELETE_OWN";

    // 文件权限
    public static final String FILE_CREATE = "FILE_CREATE";
    public static final String FILE_READ = "FILE_READ";
    public static final String FILE_UPDATE = "FILE_UPDATE";
    public static final String FILE_DELETE = "FILE_DELETE";
    public static final String FILE_UPLOAD = "FILE_UPLOAD";
    public static final String FILE_DOWNLOAD = "FILE_DOWNLOAD";
    public static final String FILE_SHARE = "FILE_SHARE";

    // 文件自有权限（普通用户）
    public static final String FILE_CREATE_OWN = "FILE_CREATE_OWN";
    public static final String FILE_READ_OWN = "FILE_READ_OWN";
    public static final String FILE_UPDATE_OWN = "FILE_UPDATE_OWN";
    public static final String FILE_DELETE_OWN = "FILE_DELETE_OWN";
    public static final String FILE_UPLOAD_OWN = "FILE_UPLOAD_OWN";
    public static final String FILE_SHARE_OWN = "FILE_SHARE_OWN";

    // 关注权限
    public static final String FOLLOW_CREATE = "FOLLOW_CREATE";
    public static final String FOLLOW_READ = "FOLLOW_READ";
    public static final String FOLLOW_DELETE = "FOLLOW_DELETE";

    // 关注自有权限（普通用户）
    public static final String FOLLOW_CREATE_OWN = "FOLLOW_CREATE_OWN";
    public static final String FOLLOW_READ_OWN = "FOLLOW_READ_OWN";
    public static final String FOLLOW_DELETE_OWN = "FOLLOW_DELETE_OWN";

    // 用户关注权限（额外的用户关注相关权限）
    public static final String USER_FOLLOW_OWN = "USER_FOLLOW_OWN";

    // 认证权限
    public static final String AUTH_LOGIN = "AUTH_LOGIN";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String AUTH_REGISTER = "AUTH_REGISTER";
    public static final String AUTH_PASSWORD_CHANGE = "AUTH_PASSWORD_CHANGE";
    public static final String AUTH_PASSWORD_RESET = "AUTH_PASSWORD_RESET";

    // 认证自有权限（普通用户）
    public static final String AUTH_LOGIN_OWN = "AUTH_LOGIN_OWN";
    public static final String AUTH_LOGOUT_OWN = "AUTH_LOGOUT_OWN";
    public static final String AUTH_PASSWORD_CHANGE_OWN = "AUTH_PASSWORD_CHANGE_OWN";

    // AI服务权限
    public static final String AI_CHAT_OWN = "AI_CHAT_OWN";
    public static final String AI_MANAGE_ANY = "AI_MANAGE_ANY";

    // 内容分析权限
    public static final String CONTENT_ANALYZE_OWN = "CONTENT_ANALYZE_OWN";

    // 内容举报权限
    public static final String CONTENT_REPORT_CREATE_OWN = "CONTENT_REPORT_CREATE_OWN";
    public static final String CONTENT_REPORT_UPDATE_OWN = "CONTENT_REPORT_UPDATE_OWN";
    public static final String CONTENT_REPORT_READ_OWN = "CONTENT_REPORT_READ_OWN";

    // 内容审核权限
    public static final String CONTENT_REVIEW_ANY = "CONTENT_REVIEW_ANY";
    public static final String CONTENT_MODERATE_ANY = "CONTENT_MODERATE_ANY";
    public static final String CONTENT_REPORT_READ = "CONTENT_REPORT_READ";
    public static final String CONTENT_REPORT_PROCESS = "CONTENT_REPORT_PROCESS";

    // ==================== 扩展用户权限等级 ====================

    // 高级用户权限（在普通用户基础上扩展）
    public static final String ADVANCED_USER_GROUP_CREATE_LIMITED = "ADVANCED_USER_GROUP_CREATE_LIMITED";
    public static final String ADVANCED_USER_ARTICLE_FEATURE_OWN = "ADVANCED_USER_ARTICLE_FEATURE_OWN";
    public static final String ADVANCED_USER_MESSAGE_THREAD_OWN = "ADVANCED_USER_MESSAGE_THREAD_OWN";
    public static final String ADVANCED_USER_CONTACT_LIMIT_INCREASED = "ADVANCED_USER_CONTACT_LIMIT_INCREASED";
    public static final String ADVANCED_USER_SEARCH_ENHANCED = "ADVANCED_USER_SEARCH_ENHANCED";
    public static final String ADVANCED_USER_NOTIFICATION_PREFERENCES = "ADVANCED_USER_NOTIFICATION_PREFERENCES";
    public static final String ADVANCED_USER_EXPORT_DATA_OWN = "ADVANCED_USER_EXPORT_DATA_OWN";

    // 活跃用户权限（在高级用户基础上扩展）
    public static final String ACTIVE_USER_GROUP_CREATE_EXTENDED = "ACTIVE_USER_GROUP_CREATE_EXTENDED";
    public static final String ACTIVE_USER_ARTICLE_MODERATE_ASSIST = "ACTIVE_USER_ARTICLE_MODERATE_ASSIST";
    public static final String ACTIVE_USER_MESSAGE_BROADCAST_LIMITED = "ACTIVE_USER_MESSAGE_BROADCAST_LIMITED";
    public static final String ACTIVE_USER_ANALYTICS_VIEW_OWN = "ACTIVE_USER_ANALYTICS_VIEW_OWN";
    public static final String ACTIVE_USER_CONTENT_MANAGEMENT_EXTENDED = "ACTIVE_USER_CONTENT_MANAGEMENT_EXTENDED";
    public static final String ACTIVE_USER_API_ACCESS_BASIC = "ACTIVE_USER_API_ACCESS_BASIC";
    public static final String ACTIVE_USER_CUSTOM_PROFILE = "ACTIVE_USER_CUSTOM_PROFILE";
    public static final String ACTIVE_USER_INVITE_USERS = "ACTIVE_USER_INVITE_USERS";

    // VIP用户权限（在活跃用户基础上扩展）
    public static final String VIP_USER_GROUP_CREATE_UNLIMITED = "VIP_USER_GROUP_CREATE_UNLIMITED";
    public static final String VIP_USER_ARTICLE_PRIORITY = "VIP_USER_ARTICLE_PRIORITY";
    public static final String VIP_USER_MESSAGE_PIN_OWN = "VIP_USER_MESSAGE_PIN_OWN";
    public static final String VIP_USER_ANALYTICS_ADVANCED = "VIP_USER_ANALYTICS_ADVANCED";
    public static final String VIP_USER_CONTENT_MANAGEMENT_FULL = "VIP_USER_CONTENT_MANAGEMENT_FULL";
    public static final String VIP_USER_API_ACCESS_EXTENDED = "VIP_USER_API_ACCESS_EXTENDED";
    public static final String VIP_USER_CUSTOM_THEME = "VIP_USER_CUSTOM_THEME";
    public static final String VIP_USER_PRIORITY_SUPPORT = "VIP_USER_PRIORITY_SUPPORT";
    public static final String VIP_USER_BETA_FEATURES = "VIP_USER_BETA_FEATURES";
    public static final String VIP_USER_EXTENDED_STORAGE = "VIP_USER_EXTENDED_STORAGE";

    // 内容创作者权限
    public static final String CONTENT_CREATOR_ARTICLE_MONETIZE = "CONTENT_CREATOR_ARTICLE_MONETIZE";
    public static final String CONTENT_CREATOR_ANALYTICS_DETAILED = "CONTENT_CREATOR_ANALYTICS_DETAILED";
    public static final String CONTENT_CREATOR_BRAND_CUSTOMIZATION = "CONTENT_CREATOR_BRAND_CUSTOMIZATION";
    public static final String CONTENT_CREATOR_FOLLOWER_ANALYTICS = "CONTENT_CREATOR_FOLLOWER_ANALYTICS";
    public static final String CONTENT_CREATOR_SCHEDULED_POSTING = "CONTENT_CREATOR_SCHEDULED_POSTING";
    public static final String CONTENT_CREATOR_SPONSORED_CONTENT = "CONTENT_CREATOR_SPONSORED_CONTENT";

    // 社区管理员权限（协助管理）
    public static final String COMMUNITY_MODERATE_CONTENT = "COMMUNITY_MODERATE_CONTENT";
    public static final String COMMUNITY_MANAGE_REPORTS = "COMMUNITY_MANAGE_REPORTS";
    public static final String COMMUNITY_WARN_USERS = "COMMUNITY_WARN_USERS";
    public static final String COMMUNITY_FEATURE_CONTENT = "COMMUNITY_FEATURE_CONTENT";
    public static final String COMMUNITY_ANNOUNCE_LOCAL = "COMMUNITY_ANNOUNCE_LOCAL";
    public static final String COMMUNITY_ANALYTICS_BASIC = "COMMUNITY_ANALYTICS_BASIC";
    public static final String COMMUNITY_ESCALATE_ISSUES = "COMMUNITY_ESCALATE_ISSUES";

    // 用户等级升级权限
    public static final String LEVEL_UP_VIEW_REQUIREMENTS = "LEVEL_UP_VIEW_REQUIREMENTS";
    public static final String LEVEL_UP_TRACK_PROGRESS = "LEVEL_UP_TRACK_PROGRESS";
    public static final String LEVEL_UP_NOTIFICATIONS = "LEVEL_UP_NOTIFICATIONS";

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

            // 用户自有权限
            USER_READ_OWN, USER_UPDATE_OWN, USER_EDIT_PROFILE_OWN, USER_SETTINGS_OWN, USER_FOLLOW_OWN,

            // 文章管理
            ARTICLE_CREATE, ARTICLE_READ, ARTICLE_UPDATE, ARTICLE_DELETE,
            ARTICLE_DELETE_OWN, ARTICLE_DELETE_ANY, ARTICLE_PUBLISH, ARTICLE_UNPUBLISH,
            ARTICLE_FEATURE, ARTICLE_LIKE, ARTICLE_FAVORITE, ARTICLE_COMMENT,

            // 文章自有权限
            ARTICLE_CREATE_OWN, ARTICLE_READ_OWN, ARTICLE_UPDATE_OWN, ARTICLE_DELETE_OWN_USER,
            ARTICLE_FAVORITE_OWN, ARTICLE_COMMENT_OWN,

            // 评论管理
            COMMENT_CREATE, COMMENT_READ, COMMENT_UPDATE, COMMENT_DELETE,
            COMMENT_DELETE_OWN, COMMENT_DELETE_ANY, COMMENT_MODERATE,

            // 群组管理
            GROUP_CREATE, GROUP_READ, GROUP_UPDATE, GROUP_DELETE,
            GROUP_DELETE_OWN, GROUP_DELETE_ANY, GROUP_JOIN, GROUP_LEAVE,
            GROUP_MANAGE, GROUP_MODERATE,

            // 群组自有权限
            GROUP_CREATE_OWN, GROUP_READ_OWN, GROUP_UPDATE_OWN, GROUP_JOIN_OWN, GROUP_LEAVE_OWN,

            // 系统管理
            SYSTEM_ADMIN, SYSTEM_CONFIG, SYSTEM_LOG, SYSTEM_BACKUP, SYSTEM_RESTORE, SYSTEM_MONITOR,

            // 权限管理
            PERMISSION_CREATE, PERMISSION_READ, PERMISSION_UPDATE, PERMISSION_DELETE,
            ROLE_CREATE, ROLE_READ, ROLE_UPDATE, ROLE_DELETE, ROLE_ASSIGN, ROLE_UNASSIGN,

            // 搜索
            SEARCH_USER, SEARCH_ARTICLE, SEARCH_GROUP, SEARCH_ADVANCED,
            SEARCH_BASIC, SEARCH_USER_BASIC, SEARCH_CONTENT_BASIC,

            // 消息
            MESSAGE_SEND, MESSAGE_READ, MESSAGE_DELETE, MESSAGE_DELETE_OWN, MESSAGE_DELETE_ANY,
            MESSAGE_RECALL, MESSAGE_RECALL_OWN, MESSAGE_RECALL_ANY,

            // 消息自有权限
            MESSAGE_CREATE_OWN, MESSAGE_READ_OWN, MESSAGE_UPDATE_OWN, MESSAGE_DELETE_OWN_USER,

            // 通知
            NOTIFICATION_SEND, NOTIFICATION_READ, NOTIFICATION_DELETE, NOTIFICATION_MANAGE,

            // 安全
            SECURITY_AUDIT, SECURITY_CONFIG, SECURITY_SESSION, SECURITY_SESSION_KILL,
            SECURITY_PASSWORD,

            // API
            API_ACCESS, API_ADMIN, API_WEBHOOK,

            // 联系人权限
            CONTACT_CREATE, CONTACT_READ, CONTACT_UPDATE, CONTACT_DELETE,
            CONTACT_CREATE_OWN, CONTACT_READ_OWN, CONTACT_UPDATE_OWN, CONTACT_DELETE_OWN,

            // 文件权限
            FILE_CREATE, FILE_READ, FILE_UPDATE, FILE_DELETE, FILE_UPLOAD, FILE_DOWNLOAD, FILE_SHARE,
            FILE_CREATE_OWN, FILE_READ_OWN, FILE_UPDATE_OWN, FILE_DELETE_OWN, FILE_UPLOAD_OWN, FILE_SHARE_OWN,

            // 关注权限
            FOLLOW_CREATE, FOLLOW_READ, FOLLOW_DELETE,
            FOLLOW_CREATE_OWN, FOLLOW_READ_OWN, FOLLOW_DELETE_OWN,

            // 认证权限
            AUTH_LOGIN, AUTH_LOGOUT, AUTH_REGISTER, AUTH_PASSWORD_CHANGE, AUTH_PASSWORD_RESET,
            AUTH_LOGIN_OWN, AUTH_LOGOUT_OWN, AUTH_PASSWORD_CHANGE_OWN,

            // AI服务权限
            AI_CHAT_OWN, AI_MANAGE_ANY,

            // 内容分析权限
            CONTENT_ANALYZE_OWN,

            // 内容举报权限
            CONTENT_REPORT_CREATE_OWN, CONTENT_REPORT_UPDATE_OWN, CONTENT_REPORT_READ_OWN,

            // 内容审核权限
            CONTENT_REVIEW_ANY, CONTENT_MODERATE_ANY, CONTENT_REPORT_READ, CONTENT_REPORT_PROCESS,

            // 扩展用户等级权限
            ADVANCED_USER_GROUP_CREATE_LIMITED, ADVANCED_USER_ARTICLE_FEATURE_OWN,
            ADVANCED_USER_MESSAGE_THREAD_OWN, ADVANCED_USER_CONTACT_LIMIT_INCREASED,
            ADVANCED_USER_SEARCH_ENHANCED, ADVANCED_USER_NOTIFICATION_PREFERENCES,
            ADVANCED_USER_EXPORT_DATA_OWN,

            ACTIVE_USER_GROUP_CREATE_EXTENDED, ACTIVE_USER_ARTICLE_MODERATE_ASSIST,
            ACTIVE_USER_MESSAGE_BROADCAST_LIMITED, ACTIVE_USER_ANALYTICS_VIEW_OWN,
            ACTIVE_USER_CONTENT_MANAGEMENT_EXTENDED, ACTIVE_USER_API_ACCESS_BASIC,
            ACTIVE_USER_CUSTOM_PROFILE, ACTIVE_USER_INVITE_USERS,

            VIP_USER_GROUP_CREATE_UNLIMITED, VIP_USER_ARTICLE_PRIORITY,
            VIP_USER_MESSAGE_PIN_OWN, VIP_USER_ANALYTICS_ADVANCED,
            VIP_USER_CONTENT_MANAGEMENT_FULL, VIP_USER_API_ACCESS_EXTENDED,
            VIP_USER_CUSTOM_THEME, VIP_USER_PRIORITY_SUPPORT,
            VIP_USER_BETA_FEATURES, VIP_USER_EXTENDED_STORAGE,

            CONTENT_CREATOR_ARTICLE_MONETIZE, CONTENT_CREATOR_ANALYTICS_DETAILED,
            CONTENT_CREATOR_BRAND_CUSTOMIZATION, CONTENT_CREATOR_FOLLOWER_ANALYTICS,
            CONTENT_CREATOR_SCHEDULED_POSTING, CONTENT_CREATOR_SPONSORED_CONTENT,

            COMMUNITY_MODERATE_CONTENT, COMMUNITY_MANAGE_REPORTS,
            COMMUNITY_WARN_USERS, COMMUNITY_FEATURE_CONTENT, COMMUNITY_ANNOUNCE_LOCAL,
            COMMUNITY_ANALYTICS_BASIC, COMMUNITY_ESCALATE_ISSUES,

            LEVEL_UP_VIEW_REQUIREMENTS, LEVEL_UP_TRACK_PROGRESS, LEVEL_UP_NOTIFICATIONS
        };
    }
}
