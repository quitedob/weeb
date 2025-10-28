package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.UserLevelHistoryMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.model.UserLevelHistory;
import com.web.service.UserLevelHistoryService;
import com.web.vo.userlevel.UserLevelHistoryQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户等级变更历史服务实现类
 */
@Slf4j
@Service
@Transactional
public class UserLevelHistoryServiceImpl implements UserLevelHistoryService {

    @Autowired
    private UserLevelHistoryMapper userLevelHistoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean recordLevelChange(Long userId, Integer oldLevel, Integer newLevel,
                                    String changeReason, Integer changeType,
                                    Long operatorId, String ipAddress, String userAgent) {
        try {
            // 验证用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            // 构建等级变更记录
            UserLevelHistory history = UserLevelHistory.builder()
                    .userId(userId)
                    .oldLevel(oldLevel)
                    .newLevel(newLevel)
                    .changeReason(changeReason)
                    .changeType(changeType)
                    .operatorId(operatorId)
                    .changeTime(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 如果有操作者，获取操作者名称
            if (operatorId != null) {
                User operator = userMapper.selectById(operatorId);
                if (operator != null) {
                    history.setOperatorName(operator.getUsername());
                }
            }

            int result = userLevelHistoryMapper.insert(history);
            if (result > 0) {
                log.info("记录用户等级变更成功: userId={}, oldLevel={}, newLevel={}, changeType={}",
                        userId, oldLevel, newLevel, changeType);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("记录用户等级变更失败: userId={}, oldLevel={}, newLevel={}",
                    userId, oldLevel, newLevel, e);
            throw new WeebException("记录等级变更失败: " + e.getMessage());
        }
    }

    @Override
    public UserLevelHistory getById(Long id) {
        try {
            return userLevelHistoryMapper.findById(id);
        } catch (Exception e) {
            log.error("查询等级变更记录失败: id={}", id, e);
            throw new WeebException("查询记录失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserLevelHistory(Long userId, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 验证用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            int offset = (page - 1) * pageSize;
            List<UserLevelHistory> histories = userLevelHistoryMapper.findByUserIdWithPaging(
                    userId, offset, pageSize);
            long total = userLevelHistoryMapper.countByUserId(userId);

            result.put("list", histories);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (total + pageSize - 1) / pageSize);

            return result;
        } catch (Exception e) {
            log.error("查询用户等级历史失败: userId={}, page={}, pageSize={}",
                    userId, page, pageSize, e);
            throw new WeebException("查询等级历史失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> queryLevelHistory(UserLevelHistoryQueryVo queryVo) {
        Map<String, Object> result = new HashMap<>();

        try {
            int offset = (queryVo.getPage() - 1) * queryVo.getPageSize();

            List<UserLevelHistory> histories = userLevelHistoryMapper.findWithPaging(
                    offset,
                    queryVo.getPageSize(),
                    queryVo.getUserId(),
                    queryVo.getChangeType(),
                    queryVo.getOperatorId(),
                    queryVo.getStartTime(),
                    queryVo.getEndTime()
            );

            long total = userLevelHistoryMapper.count(
                    queryVo.getUserId(),
                    queryVo.getChangeType(),
                    queryVo.getOperatorId(),
                    queryVo.getStartTime(),
                    queryVo.getEndTime()
            );

            result.put("list", histories);
            result.put("total", total);
            result.put("page", queryVo.getPage());
            result.put("pageSize", queryVo.getPageSize());
            result.put("totalPages", (total + queryVo.getPageSize() - 1) / queryVo.getPageSize());

            return result;
        } catch (Exception e) {
            log.error("查询等级历史失败: queryVo={}", queryVo, e);
            throw new WeebException("查询等级历史失败: " + e.getMessage());
        }
    }

    @Override
    public List<UserLevelHistory> getRecentHistory(Long userId, int limit) {
        try {
            return userLevelHistoryMapper.findRecentByUserId(userId, limit);
        } catch (Exception e) {
            log.error("查询最近等级历史失败: userId={}, limit={}", userId, limit, e);
            throw new WeebException("查询最近历史失败: " + e.getMessage());
        }
    }

    @Override
    public Integer getCurrentLevel(Long userId) {
        try {
            return userLevelHistoryMapper.getCurrentLevelByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户当前等级失败: userId={}", userId, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getUserLevelStats(Long userId, int days) {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<UserLevelHistory> histories = userLevelHistoryMapper.getUserLevelStats(userId, days);

            // 统计等级提升次数
            long levelUpCount = histories.stream()
                    .filter(h -> h.getNewLevel() > h.getOldLevel())
                    .count();

            // 统计等级降低次数
            long levelDownCount = histories.stream()
                    .filter(h -> h.getNewLevel() < h.getOldLevel())
                    .count();

            // 获取当前等级
            Integer currentLevel = getCurrentLevel(userId);

            stats.put("userId", userId);
            stats.put("days", days);
            stats.put("totalChanges", histories.size());
            stats.put("levelUpCount", levelUpCount);
            stats.put("levelDownCount", levelDownCount);
            stats.put("currentLevel", currentLevel);
            stats.put("histories", histories);

            return stats;
        } catch (Exception e) {
            log.error("获取用户等级统计失败: userId={}, days={}", userId, days, e);
            throw new WeebException("获取等级统计失败: " + e.getMessage());
        }
    }

    @Override
    public List<UserLevelHistory> getLevelUpRecords(Long userId, LocalDateTime startTime,
                                                    LocalDateTime endTime, int limit) {
        try {
            return userLevelHistoryMapper.findLevelUpRecords(userId, startTime, endTime, limit);
        } catch (Exception e) {
            log.error("查询等级提升记录失败: userId={}", userId, e);
            throw new WeebException("查询等级提升记录失败: " + e.getMessage());
        }
    }

    @Override
    public List<UserLevelHistory> getLevelDownRecords(Long userId, LocalDateTime startTime,
                                                      LocalDateTime endTime, int limit) {
        try {
            return userLevelHistoryMapper.findLevelDownRecords(userId, startTime, endTime, limit);
        } catch (Exception e) {
            log.error("查询等级降低记录失败: userId={}", userId, e);
            throw new WeebException("查询等级降低记录失败: " + e.getMessage());
        }
    }

    @Override
    public long countUserLevelChanges(Long userId) {
        try {
            return userLevelHistoryMapper.countByUserId(userId);
        } catch (Exception e) {
            log.error("统计用户等级变更次数失败: userId={}", userId, e);
            return 0;
        }
    }

    @Override
    public int cleanupExpiredRecords(LocalDateTime beforeTime) {
        try {
            int count = userLevelHistoryMapper.cleanupExpiredRecords(beforeTime);
            log.info("清理过期等级变更记录完成: count={}, beforeTime={}", count, beforeTime);
            return count;
        } catch (Exception e) {
            log.error("清理过期记录失败: beforeTime={}", beforeTime, e);
            throw new WeebException("清理过期记录失败: " + e.getMessage());
        }
    }

    @Override
    public boolean batchRecordLevelChanges(List<UserLevelHistory> histories) {
        try {
            if (histories == null || histories.isEmpty()) {
                return true;
            }

            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            for (UserLevelHistory history : histories) {
                if (history.getCreatedAt() == null) {
                    history.setCreatedAt(now);
                }
                if (history.getUpdatedAt() == null) {
                    history.setUpdatedAt(now);
                }
                if (history.getChangeTime() == null) {
                    history.setChangeTime(now);
                }
                if (history.getStatus() == null) {
                    history.setStatus(1);
                }
            }

            int result = userLevelHistoryMapper.batchInsert(histories);
            log.info("批量记录等级变更成功: count={}", result);
            return result > 0;
        } catch (Exception e) {
            log.error("批量记录等级变更失败: count={}", histories.size(), e);
            throw new WeebException("批量记录等级变更失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteUserHistory(Long userId) {
        try {
            int result = userLevelHistoryMapper.deleteByUserId(userId);
            log.info("删除用户等级历史成功: userId={}, count={}", userId, result);
            return result > 0;
        } catch (Exception e) {
            log.error("删除用户等级历史失败: userId={}", userId, e);
            throw new WeebException("删除用户历史失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        try {
            int result = userLevelHistoryMapper.updateStatus(id, status);
            return result > 0;
        } catch (Exception e) {
            log.error("更新记录状态失败: id={}, status={}", id, status, e);
            throw new WeebException("更新记录状态失败: " + e.getMessage());
        }
    }
}
