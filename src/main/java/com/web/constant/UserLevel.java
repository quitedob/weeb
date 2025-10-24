package com.web.constant;

import java.util.*;

/**
 * 用户等级常量类
 * 定义系统中所有用户等级及其权限要求
 */
public class UserLevel {

    // 用户等级枚举
    public static final int LEVEL_NEW_USER = 0;           // 新用户
    public static final int LEVEL_BASIC_USER = 1;         // 基础用户（普通用户）
    public static final int LEVEL_ADVANCED_USER = 2;      // 高级用户
    public static final int LEVEL_ACTIVE_USER = 3;        // 活跃用户
    public static final int LEVEL_VIP_USER = 4;           // VIP用户
    public static final int LEVEL_CONTENT_CREATOR = 5;    // 内容创作者
    public static final int LEVEL_COMMUNITY_MODERATOR = 6; // 社区管理员
    public static final int LEVEL_ADMIN = 7;              // 管理员
    public static final int LEVEL_SUPER_ADMIN = 8;         // 超级管理员

    // 等级名称映射
    private static final Map<Integer, String> LEVEL_NAMES = new HashMap<>();
    static {
        LEVEL_NAMES.put(LEVEL_NEW_USER, "新用户");
        LEVEL_NAMES.put(LEVEL_BASIC_USER, "普通用户");
        LEVEL_NAMES.put(LEVEL_ADVANCED_USER, "高级用户");
        LEVEL_NAMES.put(LEVEL_ACTIVE_USER, "活跃用户");
        LEVEL_NAMES.put(LEVEL_VIP_USER, "VIP用户");
        LEVEL_NAMES.put(LEVEL_CONTENT_CREATOR, "内容创作者");
        LEVEL_NAMES.put(LEVEL_COMMUNITY_MODERATOR, "社区管理员");
        LEVEL_NAMES.put(LEVEL_ADMIN, "管理员");
        LEVEL_NAMES.put(LEVEL_SUPER_ADMIN, "超级管理员");
    }

    // 等级颜色映射（用于UI显示）
    private static final Map<Integer, String> LEVEL_COLORS = new HashMap<>();
    static {
        LEVEL_COLORS.put(LEVEL_NEW_USER, "#909399");           // 灰色
        LEVEL_COLORS.put(LEVEL_BASIC_USER, "#409EFF");        // 蓝色
        LEVEL_COLORS.put(LEVEL_ADVANCED_USER, "#67C23A");     // 绿色
        LEVEL_COLORS.put(LEVEL_ACTIVE_USER, "#E6A23C");       // 橙色
        LEVEL_COLORS.put(LEVEL_VIP_USER, "#F56C6C");          // 红色
        LEVEL_COLORS.put(LEVEL_CONTENT_CREATOR, "#9B59B6");  // 紫色
        LEVEL_COLORS.put(LEVEL_COMMUNITY_MODERATOR, "#E67E22"); // 深橙色
        LEVEL_COLORS.put(LEVEL_ADMIN, "#34495E");            // 深灰色
        LEVEL_COLORS.put(LEVEL_SUPER_ADMIN, "#8E44AD");      // 深紫色
    }

    /**
     * 获取等级名称
     * @param level 等级数值
     * @return 等级名称
     */
    public static String getLevelName(int level) {
        return LEVEL_NAMES.getOrDefault(level, "未知等级");
    }

    /**
     * 获取等级颜色
     * @param level 等级数值
     * @return 颜色代码
     */
    public static String getLevelColor(int level) {
        return LEVEL_COLORS.getOrDefault(level, "#909399");
    }

    /**
     * 获取等级升级条件
     * @param targetLevel 目标等级
     * @return 升级条件Map
     */
    public static Map<String, Object> getLevelRequirements(int targetLevel) {
        Map<String, Object> requirements = new HashMap<>();

        switch (targetLevel) {
            case LEVEL_ADVANCED_USER:
                requirements.put("minArticles", 10);
                requirements.put("minMessages", 100);
                requirements.put("minLoginDays", 30);
                requirements.put("minLikes", 50);
                requirements.put("description", "发布10篇文章，发送100条消息，登录30天，获得50个点赞");
                break;

            case LEVEL_ACTIVE_USER:
                requirements.put("minArticles", 50);
                requirements.put("minMessages", 500);
                requirements.put("minLoginDays", 90);
                requirements.put("minLikes", 200);
                requirements.put("minFollowers", 10);
                requirements.put("description", "发布50篇文章，发送500条消息，登录90天，获得200个点赞，10个关注者");
                break;

            case LEVEL_VIP_USER:
                requirements.put("minArticles", 100);
                requirements.put("minMessages", 1000);
                requirements.put("minLoginDays", 180);
                requirements.put("minLikes", 500);
                requirements.put("minFollowers", 50);
                requirements.put("paymentRequired", true);
                requirements.put("description", "满足活跃用户条件并支付VIP费用");
                break;

            case LEVEL_CONTENT_CREATOR:
                requirements.put("minArticles", 200);
                requirements.put("minViews", 10000);
                requirements.put("minEngagement", 0.05); // 5%互动率
                requirements.put("contentQuality", true);
                requirements.put("description", "发布200篇文章，总浏览量超过1万，互动率5%以上，内容质量审核通过");
                break;

            case LEVEL_COMMUNITY_MODERATOR:
                requirements.put("minLevel", LEVEL_ACTIVE_USER);
                requirements.put("minReputation", 1000);
                requirements.put("noViolations", true);
                requirements.put("applicationRequired", true);
                requirements.put("description", "达到活跃用户等级，声誉值1000以上，无违规记录，提交管理员申请");
                break;

            case LEVEL_ADMIN:
                requirements.put("appointmentOnly", true);
                requirements.put("systemApproval", true);
                requirements.put("description", "仅限系统任命");
                break;

            case LEVEL_SUPER_ADMIN:
                requirements.put("appointmentOnly", true);
                requirements.put("superAdminApproval", true);
                requirements.put("description", "仅限超级管理员任命");
                break;

            default:
                requirements.put("description", "该等级暂无特殊要求");
                break;
        }

        return requirements;
    }

    /**
     * 获取等级权限列表
     * @param level 等级数值
     * @return 权限列表
     */
    public static List<String> getLevelPermissions(int level) {
        List<String> permissions = new ArrayList<>();

        // 基础权限（所有用户都有）
        permissions.addAll(Arrays.asList(
            Permissions.USER_READ_OWN,
            Permissions.USER_UPDATE_OWN,
            Permissions.USER_EDIT_PROFILE_OWN,
            Permissions.USER_SETTINGS_OWN,

            Permissions.ARTICLE_CREATE_OWN,
            Permissions.ARTICLE_READ_OWN,
            Permissions.ARTICLE_UPDATE_OWN,
            Permissions.ARTICLE_DELETE_OWN_USER,
            Permissions.ARTICLE_FAVORITE_OWN,
            Permissions.ARTICLE_COMMENT_OWN,
            Permissions.ARTICLE_LIKE,

            Permissions.MESSAGE_CREATE_OWN,
            Permissions.MESSAGE_READ_OWN,
            Permissions.MESSAGE_UPDATE_OWN,
            Permissions.MESSAGE_DELETE_OWN_USER,
            Permissions.MESSAGE_RECALL_OWN,

            Permissions.GROUP_CREATE_OWN,
            Permissions.GROUP_READ_OWN,
            Permissions.GROUP_UPDATE_OWN,
            Permissions.GROUP_JOIN_OWN,
            Permissions.GROUP_LEAVE_OWN,

            Permissions.SEARCH_BASIC,
            Permissions.SEARCH_USER_BASIC,
            Permissions.SEARCH_CONTENT_BASIC,

            Permissions.CONTACT_CREATE_OWN,
            Permissions.CONTACT_READ_OWN,
            Permissions.CONTACT_UPDATE_OWN,
            Permissions.CONTACT_DELETE_OWN,

            Permissions.FOLLOW_CREATE_OWN,
            Permissions.FOLLOW_READ_OWN,
            Permissions.FOLLOW_DELETE_OWN,

            Permissions.AUTH_LOGIN_OWN,
            Permissions.AUTH_LOGOUT_OWN,
            Permissions.AUTH_PASSWORD_CHANGE_OWN,

            Permissions.NOTIFICATION_READ,
            Permissions.LEVEL_UP_VIEW_REQUIREMENTS,
            Permissions.LEVEL_UP_TRACK_PROGRESS,
            Permissions.LEVEL_UP_NOTIFICATIONS
        ));

        // 根据等级添加额外权限
        if (level >= LEVEL_ADVANCED_USER) {
            permissions.addAll(Arrays.asList(
                Permissions.ADVANCED_USER_GROUP_CREATE_LIMITED,
                Permissions.ADVANCED_USER_ARTICLE_FEATURE_OWN,
                Permissions.ADVANCED_USER_MESSAGE_THREAD_OWN,
                Permissions.ADVANCED_USER_CONTACT_LIMIT_INCREASED,
                Permissions.ADVANCED_USER_SEARCH_ENHANCED,
                Permissions.ADVANCED_USER_NOTIFICATION_PREFERENCES,
                Permissions.ADVANCED_USER_EXPORT_DATA_OWN
            ));
        }

        if (level >= LEVEL_ACTIVE_USER) {
            permissions.addAll(Arrays.asList(
                Permissions.ACTIVE_USER_GROUP_CREATE_EXTENDED,
                Permissions.ACTIVE_USER_ARTICLE_MODERATE_ASSIST,
                Permissions.ACTIVE_USER_MESSAGE_BROADCAST_LIMITED,
                Permissions.ACTIVE_USER_ANALYTICS_VIEW_OWN,
                Permissions.ACTIVE_USER_CONTENT_MANAGEMENT_EXTENDED,
                Permissions.ACTIVE_USER_API_ACCESS_BASIC,
                Permissions.ACTIVE_USER_CUSTOM_PROFILE,
                Permissions.ACTIVE_USER_INVITE_USERS
            ));
        }

        if (level >= LEVEL_VIP_USER) {
            permissions.addAll(Arrays.asList(
                Permissions.VIP_USER_GROUP_CREATE_UNLIMITED,
                Permissions.VIP_USER_ARTICLE_PRIORITY,
                Permissions.VIP_USER_MESSAGE_PIN_OWN,
                Permissions.VIP_USER_ANALYTICS_ADVANCED,
                Permissions.VIP_USER_CONTENT_MANAGEMENT_FULL,
                Permissions.VIP_USER_API_ACCESS_EXTENDED,
                Permissions.VIP_USER_CUSTOM_THEME,
                Permissions.VIP_USER_PRIORITY_SUPPORT,
                Permissions.VIP_USER_BETA_FEATURES,
                Permissions.VIP_USER_EXTENDED_STORAGE
            ));
        }

        if (level >= LEVEL_CONTENT_CREATOR) {
            permissions.addAll(Arrays.asList(
                Permissions.CONTENT_CREATOR_ARTICLE_MONETIZE,
                Permissions.CONTENT_CREATOR_ANALYTICS_DETAILED,
                Permissions.CONTENT_CREATOR_BRAND_CUSTOMIZATION,
                Permissions.CONTENT_CREATOR_FOLLOWER_ANALYTICS,
                Permissions.CONTENT_CREATOR_SCHEDULED_POSTING,
                Permissions.CONTENT_CREATOR_SPONSORED_CONTENT
            ));
        }

        if (level >= LEVEL_COMMUNITY_MODERATOR) {
            permissions.addAll(Arrays.asList(
                Permissions.COMMUNITY_MODERATE_CONTENT,
                Permissions.COMMUNITY_MANAGE_REPORTS,
                Permissions.COMMUNITY_WARN_USERS,
                Permissions.COMMUNITY_FEATURE_CONTENT,
                Permissions.COMMUNITY_ANNOUNCE_LOCAL,
                Permissions.COMMUNITY_ANALYTICS_BASIC,
                Permissions.COMMUNITY_ESCALATE_ISSUES
            ));
        }

        if (level >= LEVEL_ADMIN) {
            permissions.addAll(Arrays.asList(
                Permissions.SYSTEM_ADMIN,
                Permissions.SYSTEM_CONFIG,
                Permissions.SYSTEM_LOG,
                Permissions.SYSTEM_MONITOR,

                Permissions.USER_CREATE,
                Permissions.USER_READ,
                Permissions.USER_UPDATE,
                Permissions.USER_DELETE,
                Permissions.USER_BAN,
                Permissions.USER_UNBAN,
                Permissions.USER_RESET_PASSWORD,
                Permissions.USER_VIEW_PROFILE,
                Permissions.USER_EDIT_PROFILE,

                Permissions.ARTICLE_DELETE_ANY,
                Permissions.ARTICLE_PUBLISH,
                Permissions.ARTICLE_UNPUBLISH,
                Permissions.ARTICLE_FEATURE,

                Permissions.COMMENT_DELETE_ANY,
                Permissions.COMMENT_MODERATE,

                Permissions.GROUP_DELETE_ANY,
                Permissions.GROUP_MANAGE,
                Permissions.GROUP_MODERATE,

                Permissions.MESSAGE_DELETE_ANY,
                Permissions.MESSAGE_RECALL_ANY,

                Permissions.NOTIFICATION_SEND,
                Permissions.NOTIFICATION_MANAGE,

                Permissions.PERMISSION_CREATE,
                Permissions.PERMISSION_READ,
                Permissions.PERMISSION_UPDATE,
                Permissions.PERMISSION_DELETE,
                Permissions.ROLE_CREATE,
                Permissions.ROLE_READ,
                Permissions.ROLE_UPDATE,
                Permissions.ROLE_DELETE,
                Permissions.ROLE_ASSIGN,
                Permissions.ROLE_UNASSIGN,

                Permissions.SEARCH_ADVANCED,

                Permissions.SECURITY_AUDIT,
                Permissions.SECURITY_CONFIG,
                Permissions.SECURITY_SESSION,
                Permissions.SECURITY_SESSION_KILL,

                Permissions.API_ADMIN,

                Permissions.SYSTEM_BACKUP,
                Permissions.SYSTEM_RESTORE
            ));
        }

        if (level >= LEVEL_SUPER_ADMIN) {
            permissions.add(Permissions.SECURITY_PASSWORD);
        }

        return permissions;
    }

    /**
     * 检查用户是否有权限升级到目标等级
     * @param currentLevel 当前等级
     * @param targetLevel 目标等级
     * @param userStats 用户统计数据
     * @return 是否可以升级
     */
    public static boolean canUpgradeTo(int currentLevel, int targetLevel, Map<String, Object> userStats) {
        if (targetLevel <= currentLevel) {
            return false; // 不能降级
        }

        Map<String, Object> requirements = getLevelRequirements(targetLevel);

        // 检查文章数量
        if (requirements.containsKey("minArticles")) {
            int required = (Integer) requirements.get("minArticles");
            int actual = (Integer) userStats.getOrDefault("articleCount", 0);
            if (actual < required) return false;
        }

        // 检查消息数量
        if (requirements.containsKey("minMessages")) {
            int required = (Integer) requirements.get("minMessages");
            int actual = (Integer) userStats.getOrDefault("messageCount", 0);
            if (actual < required) return false;
        }

        // 检查登录天数
        if (requirements.containsKey("minLoginDays")) {
            int required = (Integer) requirements.get("minLoginDays");
            int actual = (Integer) userStats.getOrDefault("loginDays", 0);
            if (actual < required) return false;
        }

        // 检查点赞数量
        if (requirements.containsKey("minLikes")) {
            int required = (Integer) requirements.get("minLikes");
            int actual = (Integer) userStats.getOrDefault("likeCount", 0);
            if (actual < required) return false;
        }

        // 检查关注者数量
        if (requirements.containsKey("minFollowers")) {
            int required = (Integer) requirements.get("minFollowers");
            int actual = (Integer) userStats.getOrDefault("followerCount", 0);
            if (actual < required) return false;
        }

        // 检查浏览量
        if (requirements.containsKey("minViews")) {
            int required = (Integer) requirements.get("minViews");
            int actual = (Integer) userStats.getOrDefault("viewCount", 0);
            if (actual < required) return false;
        }

        // 检查互动率
        if (requirements.containsKey("minEngagement")) {
            double required = (Double) requirements.get("minEngagement");
            double actual = (Double) userStats.getOrDefault("engagementRate", 0.0);
            if (actual < required) return false;
        }

        // 检查声誉值
        if (requirements.containsKey("minReputation")) {
            int required = (Integer) requirements.get("minReputation");
            int actual = (Integer) userStats.getOrDefault("reputation", 0);
            if (actual < required) return false;
        }

        // 检查最低等级要求
        if (requirements.containsKey("minLevel")) {
            int required = (Integer) requirements.get("minLevel");
            if (currentLevel < required) return false;
        }

        return true;
    }

    /**
     * 获取所有等级列表
     * @return 等级信息列表
     */
    public static List<Map<String, Object>> getAllLevels() {
        List<Map<String, Object>> levels = new ArrayList<>();

        for (int level = LEVEL_NEW_USER; level <= LEVEL_SUPER_ADMIN; level++) {
            Map<String, Object> levelInfo = new HashMap<>();
            levelInfo.put("level", level);
            levelInfo.put("name", getLevelName(level));
            levelInfo.put("color", getLevelColor(level));
            levelInfo.put("permissions", getLevelPermissions(level));
            levelInfo.put("requirements", getLevelRequirements(level));
            levels.add(levelInfo);
        }

        return levels;
    }

    private UserLevel() {
        // 私有构造器，防止实例化
    }
}