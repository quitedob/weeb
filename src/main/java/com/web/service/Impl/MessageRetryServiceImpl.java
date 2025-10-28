 package com.web.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.model.Message;
import com.web.service.MessageRetryService;
import com.web.service.UnifiedMessageService;
import com.web.vo.message.SendMessageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 消息重试服务实现
 * 处理消息发送失败的重试逻辑
 */
@Slf4j
@Service
public class MessageRetryServiceImpl implements MessageRetryService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private UnifiedMessageService unifiedMessageService;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis键前缀
    private static final String FAILED_MESSAGE_KEY_PREFIX = "message:failed:";
    private static final String USER_FAILED_MESSAGES_KEY_PREFIX = "message:failed:user:";
    private static final String RETRY_STATS_KEY = "message:retry:stats";

    // 重试配置
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_SECONDS = 60; // 1分钟后重试
    private static final long FAILED_RECORD_TTL = 86400; // 24小时后过期

    @Override
    public String recordFailedMessage(SendMessageVo sendMessageVo, Long userId, String errorMessage) {
        try {
            String retryId = UUID.randomUUID().toString();
            String key = FAILED_MESSAGE_KEY_PREFIX + retryId;

            Map<String, Object> failedRecord = new HashMap<>();
            failedRecord.put("retryId", retryId);
            failedRecord.put("userId", userId);
            failedRecord.put("sendMessageVo", sendMessageVo);
            failedRecord.put("errorMessage", errorMessage);
            failedRecord.put("retryCount", 0);
            failedRecord.put("createdAt", LocalDateTime.now().toString());
            failedRecord.put("lastRetryAt", null);
            failedRecord.put("status", "PENDING");

            // 保存失败记录
            redisTemplate.opsForHash().putAll(key, failedRecord);
            redisTemplate.expire(key, FAILED_RECORD_TTL, TimeUnit.SECONDS);

            // 添加到用户的失败消息列表
            String userKey = USER_FAILED_MESSAGES_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(userKey, retryId);
            redisTemplate.expire(userKey, FAILED_RECORD_TTL, TimeUnit.SECONDS);

            // 更新统计
            redisTemplate.opsForHash().increment(RETRY_STATS_KEY, "totalFailed", 1);

            log.info("失败消息已记录: retryId={}, userId={}, error={}", retryId, userId, errorMessage);

            return retryId;

        } catch (Exception e) {
            log.error("记录失败消息失败: userId={}", userId, e);
            return null;
        }
    }

    @Override
    public boolean retryFailedMessage(String retryId) {
        if (retryId == null || retryId.trim().isEmpty()) {
            return false;
        }

        try {
            String key = FAILED_MESSAGE_KEY_PREFIX + retryId;
            Map<Object, Object> record = redisTemplate.opsForHash().entries(key);

            if (record.isEmpty()) {
                log.warn("重试记录不存在: retryId={}", retryId);
                return false;
            }

            // 检查重试次数
            int retryCount = getIntValue(record.get("retryCount"));
            if (retryCount >= MAX_RETRY_ATTEMPTS) {
                log.warn("重试次数已达上限: retryId={}, retryCount={}", retryId, retryCount);
                updateRecordStatus(key, "MAX_RETRIES_REACHED");
                return false;
            }

            // 获取消息信息
            Long userId = getLongValue(record.get("userId"));
            Object sendMessageVoObj = record.get("sendMessageVo");

            if (sendMessageVoObj == null) {
                log.error("消息信息缺失: retryId={}", retryId);
                return false;
            }

            // 转换SendMessageVo
            SendMessageVo sendMessageVo = objectMapper.convertValue(sendMessageVoObj, SendMessageVo.class);

            // 尝试重新发送
            try {
                Message message = unifiedMessageService.sendMessage(sendMessageVo, userId);

                if (message != null) {
                    // 发送成功，更新状态
                    updateRecordStatus(key, "SUCCESS");

                    // 从用户失败列表中移除
                    String userKey = USER_FAILED_MESSAGES_KEY_PREFIX + userId;
                    redisTemplate.opsForSet().remove(userKey, retryId);

                    // 更新统计
                    redisTemplate.opsForHash().increment(RETRY_STATS_KEY, "totalRetrySuccess", 1);

                    log.info("消息重试成功: retryId={}, messageId={}", retryId, message.getId());
                    return true;
                }

            } catch (Exception e) {
                // 重试失败，更新重试次数
                redisTemplate.opsForHash().increment(key, "retryCount", 1);
                redisTemplate.opsForHash().put(key, "lastRetryAt", LocalDateTime.now().toString());
                redisTemplate.opsForHash().put(key, "lastError", e.getMessage());

                // 更新统计
                redisTemplate.opsForHash().increment(RETRY_STATS_KEY, "totalRetryFailed", 1);

                log.warn("消息重试失败: retryId={}, retryCount={}, error={}",
                        retryId, retryCount + 1, e.getMessage());
            }

            return false;

        } catch (Exception e) {
            log.error("重试失败消息失败: retryId={}", retryId, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getFailedMessages(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            String userKey = USER_FAILED_MESSAGES_KEY_PREFIX + userId;
            Set<Object> retryIds = redisTemplate.opsForSet().members(userKey);

            if (retryIds == null || retryIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> failedMessages = new ArrayList<>();

            for (Object retryIdObj : retryIds) {
                String retryId = retryIdObj.toString();
                String key = FAILED_MESSAGE_KEY_PREFIX + retryId;
                Map<Object, Object> record = redisTemplate.opsForHash().entries(key);

                if (!record.isEmpty()) {
                    Map<String, Object> failedMessage = new HashMap<>();
                    record.forEach((k, v) -> failedMessage.put(k.toString(), v));
                    failedMessages.add(failedMessage);
                }
            }

            // 按创建时间倒序排序
            failedMessages.sort((m1, m2) -> {
                String time1 = (String) m1.get("createdAt");
                String time2 = (String) m2.get("createdAt");
                return time2.compareTo(time1);
            });

            return failedMessages;

        } catch (Exception e) {
            log.error("获取失败消息列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public int autoRetryFailedMessages() {
        int successCount = 0;

        try {
            // 获取所有失败记录的键
            Set<String> keys = redisTemplate.keys(FAILED_MESSAGE_KEY_PREFIX + "*");

            if (keys == null || keys.isEmpty()) {
                return 0;
            }

            for (String key : keys) {
                Map<Object, Object> record = redisTemplate.opsForHash().entries(key);

                if (record.isEmpty()) {
                    continue;
                }

                String status = (String) record.get("status");
                if (!"PENDING".equals(status)) {
                    continue;
                }

                // 检查是否到了重试时间
                String lastRetryAtStr = (String) record.get("lastRetryAt");
                if (lastRetryAtStr != null) {
                    LocalDateTime lastRetryAt = LocalDateTime.parse(lastRetryAtStr);
                    if (LocalDateTime.now().isBefore(lastRetryAt.plusSeconds(RETRY_DELAY_SECONDS))) {
                        continue; // 还没到重试时间
                    }
                }

                // 提取retryId
                String retryId = (String) record.get("retryId");
                if (retryFailedMessage(retryId)) {
                    successCount++;
                }
            }

            log.info("自动重试完成: 成功数={}", successCount);

        } catch (Exception e) {
            log.error("自动重试失败消息失败", e);
        }

        return successCount;
    }

    @Override
    public int cleanExpiredFailedRecords() {
        int cleanedCount = 0;

        try {
            // Redis的TTL会自动清理过期记录
            // 这里只需要清理状态为SUCCESS的记录
            Set<String> keys = redisTemplate.keys(FAILED_MESSAGE_KEY_PREFIX + "*");

            if (keys == null || keys.isEmpty()) {
                return 0;
            }

            for (String key : keys) {
                Map<Object, Object> record = redisTemplate.opsForHash().entries(key);

                if (record.isEmpty()) {
                    continue;
                }

                String status = (String) record.get("status");
                if ("SUCCESS".equals(status) || "MAX_RETRIES_REACHED".equals(status)) {
                    // 删除已完成的记录
                    redisTemplate.delete(key);
                    cleanedCount++;
                }
            }

            log.info("清理过期失败记录完成: 清理数={}", cleanedCount);

        } catch (Exception e) {
            log.error("清理过期失败记录失败", e);
        }

        return cleanedCount;
    }

    @Override
    public Map<String, Object> getRetryStatistics() {
        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(RETRY_STATS_KEY);
            Map<String, Object> result = new HashMap<>();

            stats.forEach((key, value) -> result.put(key.toString(), value));

            // 计算重试成功率
            long totalRetrySuccess = getLongValue(result.get("totalRetrySuccess"));
            long totalRetryFailed = getLongValue(result.get("totalRetryFailed"));
            long totalRetries = totalRetrySuccess + totalRetryFailed;

            if (totalRetries > 0) {
                double successRate = (double) totalRetrySuccess / totalRetries * 100;
                result.put("retrySuccessRate", String.format("%.2f%%", successRate));
            } else {
                result.put("retrySuccessRate", "0.00%");
            }

            result.put("timestamp", LocalDateTime.now().toString());

            return result;

        } catch (Exception e) {
            log.error("获取重试统计信息失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 更新记录状态
     */
    private void updateRecordStatus(String key, String status) {
        try {
            redisTemplate.opsForHash().put(key, "status", status);
            redisTemplate.opsForHash().put(key, "updatedAt", LocalDateTime.now().toString());
        } catch (Exception e) {
            log.error("更新记录状态失败: key={}, status={}", key, status, e);
        }
    }

    /**
     * 获取Integer值
     */
    private int getIntValue(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 获取Long值
     */
    private long getLongValue(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
