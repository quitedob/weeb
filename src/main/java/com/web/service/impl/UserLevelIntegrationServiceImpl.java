package com.web.service.impl;

import com.web.constant.UserLevel;
import com.web.exception.WeebException;
import com.web.service.UserLevelHistoryService;
import com.web.service.UserLevelIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class UserLevelIntegrationServiceImpl implements UserLevelIntegrationService {
    @Autowired private UserLevelHistoryService userLevelHistoryService;

    @Override
    public Map<String, Object> handleLevelChange(Long userId, Integer oldLevel, Integer newLevel,
                                                String reason, Integer changeType, Long operatorId,
                                                String ipAddress, String userAgent) {
        Map<String, Object> validation = validateLevelChange(userId, oldLevel, newLevel);
        if (!Boolean.TRUE.equals(validation.get("valid"))) throw new WeebException((String) validation.get("error"));
        // recordLevelChange rechecks the old level under a database lock before committing.
        boolean recorded = userLevelHistoryService.recordLevelChange(userId, oldLevel, newLevel,
                reason, changeType, operatorId, ipAddress, userAgent);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", recorded);
        result.put("historyRecorded", recorded);
        result.put("userId", userId);
        result.put("oldLevel", oldLevel);
        result.put("newLevel", recorded ? newLevel : oldLevel);
        result.put("changeReason", reason);
        result.put("changeType", operatorId == null ? changeType : 2);
        result.put("message", recorded ? "等级变更处理完成" : "等级变更保存失败");
        return result;
    }

    @Override
    public Map<String, Object> batchHandleLevelChanges(List<Map<String, Object>> changes) {
        if (changes == null || changes.size() > 100) throw new WeebException("无效的批量更新请求");
        List<Map<String, Object>> successes = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        // Each history write owns its transaction, so a failed item cannot roll back reported successes.
        for (Map<String, Object> change : changes) {
            try {
                Map<String, Object> result = handleLevelChange(integer(change.get("userId")),
                        Math.toIntExact(integer(change.get("oldLevel"))), Math.toIntExact(integer(change.get("newLevel"))),
                        (String) change.get("changeReason"), Math.toIntExact(integer(change.get("changeType"))),
                        change.get("operatorId") == null ? null : integer(change.get("operatorId")),
                        (String) change.get("ipAddress"), (String) change.get("userAgent"));
                (Boolean.TRUE.equals(result.get("success")) ? successes : failures).add(result);
            } catch (RuntimeException error) {
                Map<String, Object> failed = new HashMap<>();
                failed.put("userId", change == null ? null : change.get("userId"));
                failed.put("error", "等级更新失败，请检查当前等级和目标等级");
                failures.add(failed);
            }
        }
        return Map.of("totalCount", changes.size(), "successCount", successes.size(), "failedCount", failures.size(),
                "successResults", successes, "failedResults", failures);
    }

    private long integer(Object value) {
        if (!(value instanceof Number number)) throw new WeebException("需要整数参数");
        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw new WeebException("需要整数参数");
        }
    }

    @Override
    public Map<String, Object> getUserLevelCompleteInfo(Long userId) {
        return Map.of("userId", userId, "currentLevel", userLevelHistoryService.getCurrentLevel(userId),
                "recentHistory", userLevelHistoryService.getRecentHistory(userId, 10),
                "stats", userLevelHistoryService.getUserLevelStats(userId, 30));
    }

    @Override
    public Map<String, Object> validateLevelChange(Long userId, Integer oldLevel, Integer newLevel) {
        if (oldLevel == null || newLevel == null || !UserLevel.isValidLevel(oldLevel) || !UserLevel.isValidLevel(newLevel)) {
            return Map.of("valid", false, "error", "等级必须在0到8之间");
        }
        int current = userLevelHistoryService.getCurrentLevel(userId);
        if (current != oldLevel) return Map.of("valid", false, "error", "原等级不匹配，请刷新后重试");
        if (Math.abs(newLevel - oldLevel) > 3) return Map.of("valid", false, "error", "单次等级变更不能超过3级");
        return Map.of("valid", true, "message", "等级变更验证通过");
    }
}
