package com.web.task;

import com.web.service.WebSocketConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket连接监控定时任务
 */
@Slf4j
@Component
public class WebSocketConnectionMonitorTask {

    @Autowired
    private WebSocketConnectionService connectionService;

    /**
     * 清理过期连接
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void cleanExpiredConnections() {
        try {
            log.debug("开始清理过期WebSocket连接...");
            int cleanedCount = connectionService.cleanExpiredConnections();

            if (cleanedCount > 0) {
                log.info("清理过期WebSocket连接完成: 清理数量={}", cleanedCount);
            }

        } catch (Exception e) {
            log.error("清理过期WebSocket连接失败", e);
        }
    }

    /**
     * 记录连接统计信息
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void recordConnectionStatistics() {
        try {
            Map<String, Object> stats = connectionService.getConnectionStatistics();
            long onlineUsers = connectionService.getOnlineUserCount();

            log.info("WebSocket连接统计: 在线用户数={}, 统计信息={}", onlineUsers, stats);

        } catch (Exception e) {
            log.error("记录WebSocket连接统计失败", e);
        }
    }

    /**
     * 监控连接健康状态
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 600000) // 每10分钟执行一次
    public void monitorConnectionHealth() {
        try {
            long onlineUsers = connectionService.getOnlineUserCount();
            Map<String, Object> stats = connectionService.getConnectionStatistics();

            // 检查是否有异常情况
            Object totalConnections = stats.get("totalConnections");
            Object totalDisconnections = stats.get("totalDisconnections");

            if (totalConnections != null && totalDisconnections != null) {
                long connections = Long.parseLong(totalConnections.toString());
                long disconnections = Long.parseLong(totalDisconnections.toString());

                // 如果断开连接数异常高，记录警告
                if (connections > 0 && disconnections > connections * 0.8) {
                    log.warn("WebSocket连接异常: 断开率过高 - 总连接={}, 总断开={}, 当前在线={}",
                            connections, disconnections, onlineUsers);
                }
            }

        } catch (Exception e) {
            log.error("监控WebSocket连接健康状态失败", e);
        }
    }
}
