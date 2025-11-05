package com.web.constants;

/**
 * 群组角色常量类
 * 统一定义群组角色，避免不同模块定义不一致的问题
 */
public class GroupRoleConstants {
    
    /**
     * 群主角色
     * 拥有最高权限：可以解散群组、转让群组、管理所有成员
     */
    public static final int ROLE_OWNER = 1;
    
    /**
     * 管理员角色
     * 拥有管理权限：可以邀请成员、踢出普通成员、管理群组信息
     */
    public static final int ROLE_ADMIN = 2;
    
    /**
     * 普通成员角色
     * 基本权限：可以发送消息、查看群组信息
     */
    public static final int ROLE_MEMBER = 3;
    
    /**
     * 非成员
     * 用于标识用户不是群组成员
     */
    public static final int ROLE_NON_MEMBER = -1;
    
    /**
     * 检查角色是否有效
     * @param role 角色值
     * @return 是否有效
     */
    public static boolean isValidRole(int role) {
        return role == ROLE_OWNER || role == ROLE_ADMIN || role == ROLE_MEMBER;
    }
    
    /**
     * 检查用户角色是否满足所需角色权限
     * @param userRole 用户角色
     * @param requiredRole 所需角色
     * @return 是否满足权限要求
     */
    public static boolean hasPermission(int userRole, int requiredRole) {
        // 角色值越小，权限越高
        return userRole <= requiredRole && userRole > 0;
    }
    
    /**
     * 获取角色名称
     * @param role 角色值
     * @return 角色名称
     */
    public static String getRoleName(int role) {
        switch (role) {
            case ROLE_OWNER:
                return "群主";
            case ROLE_ADMIN:
                return "管理员";
            case ROLE_MEMBER:
                return "普通成员";
            case ROLE_NON_MEMBER:
                return "非成员";
            default:
                return "未知角色";
        }
    }
    
    /**
     * 检查是否为群主
     * @param role 角色值
     * @return 是否为群主
     */
    public static boolean isOwner(int role) {
        return role == ROLE_OWNER;
    }
    
    /**
     * 检查是否为管理员或群主
     * @param role 角色值
     * @return 是否为管理员或群主
     */
    public static boolean isAdminOrOwner(int role) {
        return role == ROLE_OWNER || role == ROLE_ADMIN;
    }
    
    /**
     * 检查是否为群组成员（包括群主、管理员、普通成员）
     * @param role 角色值
     * @return 是否为群组成员
     */
    public static boolean isMember(int role) {
        return role == ROLE_OWNER || role == ROLE_ADMIN || role == ROLE_MEMBER;
    }
    
    private GroupRoleConstants() {
        // 私有构造函数，防止实例化
    }
}
