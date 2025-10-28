package com.web.service;

import java.util.Map;

/**
 * WebSocket连接管理服务接口
 */
public interface WebSocketConnectionService {

    /**
     * 注册WebSocket连接
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param username 用户名
     */
    void registerConnection(String sessionId, Long userId, String username);

    /**
     * 注销WebSocket连接
     * @param sessionId 会话ID
     */
    void unregisterConnection(String sessionId);

    /**
     * 更新心跳时间
     * @param sessionId 会话ID
     */
    void updateHeartbeat(String sessionId);

    /**
     * 检查连接是否存活
     * @param sessionId 会话ID
     * @return 是否存活
     */
    boolean isConnectionAlive(String sessionId);

    /**
     * 获取用户的所有活跃会话
     * @param userId 用户ID
     * @return 会话ID列表
     */
    java.util.Set<String> getUserActiveSessions(Long userId);

    /**
     * 获取在线用户数
     * @return 在线用户数
     */
    long getOnlineUserCount();

    /**
     * 获取所有在线用户ID
     * @return 用户ID集合
     */
    java.util.Set<Long> getOnlineUserIds();

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean isUserOnline(Long userId);

    /**
     * 获取连接统计信息
     * @return 统计信息
     */
    Map<String, Object> getConnectionStatistics();

    /**
     * 清理过期连接
     * @return 清理的连接数
     */
    int cleanExpiredConnections();

    /**
     * 获取用户连接信息
     * @param userId 用户ID
     * @return 连接信息
     */
    Map<String, Object> getUserConnectionInfo(Long userId);
}
