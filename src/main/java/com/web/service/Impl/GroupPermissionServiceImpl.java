package com.web.service.Impl;

import com.web.mapper.GroupMemberMapper;
import com.web.model.GroupMember;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.service.GroupPermissionService;
import com.web.service.RedisCacheService;
import com.web.service.UserTypeSecurityService;
import com.web.constants.GroupRoleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 群组权限服务实现类
 * 统一管理群组相关的权限检查逻辑
 * 使用Redis缓存提高性能
 * 已修复：统一使用GroupRoleConstants定义角色
 */
@Slf4j
@Service
public class GroupPermissionServiceImpl implements GroupPermissionService {

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private UserTypeSecurityService userTypeSecurityService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RedisCacheService redisCacheService;

    // 缓存键前缀
    private static final String CACHE_PREFIX = "group:permission:";
    private static final long CACHE_EXPIRE_SECONDS = 300; // 5分钟

    /**
     * 获取缓存键
     */
    private String getCacheKey(Long groupId, Long userId) {
        return CACHE_PREFIX + groupId + ":" + userId;
    }

    /**
     * 从缓存或数据库获取用户角色
     */
    private int getUserRoleFromCacheOrDb(Long groupId, Long userId) {
        String cacheKey = getCacheKey(groupId, userId);

        try {
            // 尝试从缓存获取
            Object cachedRole = redisCacheService.get(cacheKey);
            if (cachedRole != null) {
                return (Integer) cachedRole;
            }

            // 缓存未命中，从数据库查询
            GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
            int role = (member != null && "ACCEPTED".equals(member.getJoinStatus())) 
                ? member.getRole() : GroupRoleConstants.ROLE_NON_MEMBER;

            // 存入缓存
            redisCacheService.set(cacheKey, role, CACHE_EXPIRE_SECONDS);

            return role;
        } catch (Exception e) {
            log.error("获取用户群组角色失败: groupId={}, userId={}", groupId, userId, e);
            // 缓存失败时直接查询数据库
            GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
            return (member != null && "ACCEPTED".equals(member.getJoinStatus())) 
                ? member.getRole() : GroupRoleConstants.ROLE_NON_MEMBER;
        }
    }

    @Override
    public boolean isGroupOwner(Long groupId, Long userId) {
        try {
            // 系统管理员拥有所有权限
            User currentUser = authService.findByUserID(userId);
            if (currentUser != null && userTypeSecurityService.isAdmin(currentUser.getUsername())) {
                log.debug("系统管理员拥有群主权限: userId={}, groupId={}", userId, groupId);
                return true;
            }

            int role = getUserRoleFromCacheOrDb(groupId, userId);
            return GroupRoleConstants.isOwner(role);
        } catch (Exception e) {
            log.error("检查群主权限失败: groupId={}, userId={}", groupId, userId, e);
            return false;
        }
    }

    @Override
    public boolean isGroupAdmin(Long groupId, Long userId) {
        try {
            // 系统管理员拥有所有权限
            User currentUser = authService.findByUserID(userId);
            if (currentUser != null && userTypeSecurityService.isAdmin(currentUser.getUsername())) {
                log.debug("系统管理员拥有管理员权限: userId={}, groupId={}", userId, groupId);
                return true;
            }

            int role = getUserRoleFromCacheOrDb(groupId, userId);
            return GroupRoleConstants.isAdminOrOwner(role);
        } catch (Exception e) {
            log.error("检查管理员权限失败: groupId={}, userId={}", groupId, userId, e);
            return false;
        }
    }

    @Override
    public boolean isGroupMember(Long groupId, Long userId) {
        try {
            int role = getUserRoleFromCacheOrDb(groupId, userId);
            return GroupRoleConstants.isMember(role);
        } catch (Exception e) {
            log.error("检查成员权限失败: groupId={}, userId={}", groupId, userId, e);
            return false;
        }
    }

    @Override
    public boolean canManageGroup(Long groupId, Long userId) {
        // 群主和管理员可以管理群组信息
        return isGroupAdmin(groupId, userId);
    }

    @Override
    public boolean canInviteMembers(Long groupId, Long userId) {
        // 群主和管理员可以邀请成员
        return isGroupAdmin(groupId, userId);
    }

    @Override
    public boolean canKickMember(Long groupId, Long userId, Long targetUserId) {
        try {
            // 系统管理员可以踢出任何人
            User currentUser = authService.findByUserID(userId);
            if (currentUser != null && userTypeSecurityService.isAdmin(currentUser.getUsername())) {
                return true;
            }

            // 获取操作者和目标用户的角色
            int operatorRole = getUserRoleFromCacheOrDb(groupId, userId);
            int targetRole = getUserRoleFromCacheOrDb(groupId, targetUserId);

            // 不能踢出自己
            if (userId.equals(targetUserId)) {
                return false;
            }

            // 不能踢出群主
            if (targetRole == ROLE_OWNER) {
                return false;
            }

            // 群主可以踢出任何人（除了自己）
            if (operatorRole == ROLE_OWNER) {
                return true;
            }

            // 管理员只能踢出普通成员
            if (operatorRole == ROLE_ADMIN && targetRole == ROLE_MEMBER) {
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("检查踢出权限失败: groupId={}, userId={}, targetUserId={}", groupId, userId, targetUserId, e);
            return false;
        }
    }

    @Override
    public boolean canDissolveGroup(Long groupId, Long userId) {
        // 只有群主可以解散群组
        return isGroupOwner(groupId, userId);
    }

    @Override
    public boolean canTransferGroup(Long groupId, Long userId) {
        // 只有群主可以转让群组
        return isGroupOwner(groupId, userId);
    }

    @Override
    public boolean canSendMessage(Long groupId, Long userId) {
        // 所有群组成员都可以发送消息
        return isGroupMember(groupId, userId);
    }

    @Override
    public boolean canSetMemberRole(Long groupId, Long userId, Long targetUserId, int newRole) {
        try {
            // 系统管理员可以设置任何角色
            User currentUser = authService.findByUserID(userId);
            if (currentUser != null && userTypeSecurityService.isAdmin(currentUser.getUsername())) {
                return true;
            }

            // 获取操作者和目标用户的角色
            int operatorRole = getUserRoleFromCacheOrDb(groupId, userId);
            int targetRole = getUserRoleFromCacheOrDb(groupId, targetUserId);

            // 不能修改自己的角色
            if (userId.equals(targetUserId)) {
                return false;
            }

            // 不能修改群主的角色
            if (targetRole == ROLE_OWNER) {
                return false;
            }

            // 只有群主可以设置管理员
            if (operatorRole == ROLE_OWNER) {
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("检查设置角色权限失败: groupId={}, userId={}, targetUserId={}, newRole={}",
                groupId, userId, targetUserId, newRole, e);
            return false;
        }
    }

    @Override
    public int getUserRoleInGroup(Long groupId, Long userId) {
        return getUserRoleFromCacheOrDb(groupId, userId);
    }

    @Override
    public void clearPermissionCache(Long groupId, Long userId) {
        try {
            String cacheKey = getCacheKey(groupId, userId);
            redisCacheService.delete(cacheKey);
            log.debug("清除用户群组权限缓存: groupId={}, userId={}", groupId, userId);
        } catch (Exception e) {
            log.error("清除权限缓存失败: groupId={}, userId={}", groupId, userId, e);
        }
    }

    @Override
    public void clearGroupPermissionCache(Long groupId) {
        try {
            // 清除该群组所有用户的权限缓存
            // TODO: 实现批量删除缓存功能
            // 暂时不清除缓存，等待缓存自动过期
            log.debug("群组权限缓存将在{}秒后自动过期: groupId={}", CACHE_EXPIRE_SECONDS, groupId);
        } catch (Exception e) {
            log.error("清除群组权限缓存失败: groupId={}", groupId, e);
        }
    }
}
