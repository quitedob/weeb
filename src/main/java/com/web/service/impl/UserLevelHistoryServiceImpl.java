package com.web.service.impl;

import com.web.constant.UserLevel;
import com.web.constant.UserType;
import com.web.exception.WeebException;
import com.web.mapper.UserLevelHistoryMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.model.UserLevelHistory;
import com.web.service.UserLevelHistoryService;
import com.web.vo.userlevel.UserLevelHistoryQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserLevelHistoryServiceImpl implements UserLevelHistoryService {
    @Autowired private UserLevelHistoryMapper userLevelHistoryMapper;
    @Autowired private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordLevelChange(Long userId, Integer oldLevel, Integer newLevel, String reason,
                                     Integer changeType, Long operatorId, String ipAddress, String userAgent) {
        if (newLevel == null || !UserLevel.isValidLevel(newLevel)
                || (oldLevel != null && !UserLevel.isValidLevel(oldLevel))) throw new WeebException("等级必须在0到8之间");
        if (changeType == null || changeType < 1 || changeType > 3) throw new WeebException("无效的等级变更类型");
        User operator = null;
        if (operatorId != null) {
            operator = requireUser(operatorId);
            if (!Integer.valueOf(1).equals(operator.getStatus())
                    || !UserType.ADMIN.equals(UserType.normalize(operator.getType()))) throw new WeebException("需要管理员权限");
        } else if (changeType == 2 || newLevel > UserLevel.LEVEL_CONTENT_CREATOR) {
            throw new WeebException("需要管理员操作记录");
        }
        // The user row serializes all writes, including the first history record.
        if (userId == null || userMapper.lockUserForLevelChange(userId) == null) throw new WeebException("用户不存在");
        Integer persisted = userMapper.selectCurrentLevelForUpdate(userId);
        int current = persisted == null ? UserLevel.LEVEL_BASIC_USER : persisted;
        if (oldLevel != null && oldLevel != current) throw new WeebException("等级已变更，请刷新后重试");
        LocalDateTime now = LocalDateTime.now();
        UserLevelHistory history = UserLevelHistory.builder()
                .userId(userId).oldLevel(current).newLevel(newLevel).changeReason(bounded(reason, 500))
                .changeType(operatorId == null ? changeType : 2).operatorId(operatorId)
                .operatorName(operator == null ? null : operator.getUsername())
                .changeTime(now).ipAddress(bounded(ipAddress, 50)).userAgent(bounded(userAgent, 500))
                .status(1).createdAt(now).updatedAt(now).build();
        return userLevelHistoryMapper.insert(history) == 1;
    }

    @Override
    public UserLevelHistory getById(Long id) {
        return userLevelHistoryMapper.findById(id);
    }

    @Override
    public Map<String, Object> getUserLevelHistory(Long userId, int page, int pageSize) {
        requireUser(userId);
        int offset = offset(page, pageSize);
        return page(userLevelHistoryMapper.findByUserIdWithPaging(userId, offset, pageSize),
                userLevelHistoryMapper.countByUserId(userId), page, pageSize);
    }

    @Override
    public Map<String, Object> queryLevelHistory(UserLevelHistoryQueryVo query) {
        if (query == null) throw new WeebException("查询条件不能为空");
        int offset = offset(query.getPage(), query.getPageSize());
        checkTimeRange(query.getStartTime(), query.getEndTime());
        List<UserLevelHistory> records = userLevelHistoryMapper.findWithPaging(offset, query.getPageSize(),
                query.getUserId(), query.getChangeType(), query.getOperatorId(), query.getStartTime(), query.getEndTime());
        long count = userLevelHistoryMapper.count(query.getUserId(), query.getChangeType(), query.getOperatorId(),
                query.getStartTime(), query.getEndTime());
        return page(records, count, query.getPage(), query.getPageSize());
    }

    private Map<String, Object> page(List<UserLevelHistory> records, long total, int page, int size) {
        return Map.of("list", records, "total", total, "page", page, "pageSize", size,
                "totalPages", (total + size - 1) / size);
    }

    private int offset(int page, int size) {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            throw new WeebException("无效的分页参数");
        }
        return (page - 1) * size;
    }

    private void checkLimit(int limit) {
        if (limit < 1 || limit > 100) throw new WeebException("查询数量必须在1到100之间");
    }

    private void checkTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) throw new WeebException("开始时间不能晚于结束时间");
    }

    @Override
    public List<UserLevelHistory> getRecentHistory(Long userId, int limit) {
        requireUser(userId);
        checkLimit(limit);
        return userLevelHistoryMapper.findRecentByUserId(userId, limit);
    }

    @Override
    public Integer getCurrentLevel(Long userId) {
        requireUser(userId);
        Integer current = userLevelHistoryMapper.getCurrentLevelByUserId(userId);
        return current == null ? UserLevel.LEVEL_BASIC_USER : current;
    }

    @Override
    public Map<String, Object> getUserLevelStats(Long userId, int days) {
        if (days < 1 || days > 3650) throw new WeebException("统计天数必须在1到3650之间");
        int current = getCurrentLevel(userId);
        List<UserLevelHistory> histories = userLevelHistoryMapper.getUserLevelStats(userId, days);
        long ups = histories.stream().filter(h -> h.getOldLevel() != null && h.getNewLevel() > h.getOldLevel()).count();
        long downs = histories.stream().filter(h -> h.getOldLevel() != null && h.getNewLevel() < h.getOldLevel()).count();
        return Map.of("userId", userId, "days", days, "totalChanges", histories.size(), "levelUpCount", ups,
                "levelDownCount", downs, "currentLevel", current, "histories", histories);
    }

    @Override
    public List<UserLevelHistory> getLevelUpRecords(Long userId, LocalDateTime start, LocalDateTime end, int limit) {
        checkLimit(limit);
        checkTimeRange(start, end);
        return userLevelHistoryMapper.findLevelUpRecords(userId, start, end, limit);
    }

    @Override
    public List<UserLevelHistory> getLevelDownRecords(Long userId, LocalDateTime start, LocalDateTime end, int limit) {
        checkLimit(limit);
        checkTimeRange(start, end);
        return userLevelHistoryMapper.findLevelDownRecords(userId, start, end, limit);
    }

    @Override
    public long countUserLevelChanges(Long userId) {
        requireUser(userId);
        return userLevelHistoryMapper.countByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredRecords(LocalDateTime beforeTime) {
        if (beforeTime == null || beforeTime.isAfter(LocalDateTime.now())) throw new WeebException("无效的清理截止时间");
        return userLevelHistoryMapper.cleanupExpiredRecords(beforeTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRecordLevelChanges(List<UserLevelHistory> histories) {
        if (histories == null || histories.size() > 100) throw new WeebException("无效的批量更新请求");
        List<UserLevelHistory> ordered = new ArrayList<>(histories);
        if (ordered.stream().anyMatch(h -> h == null || h.getUserId() == null)) throw new WeebException("用户不能为空");
        ordered.sort(Comparator.comparing(UserLevelHistory::getUserId));
        for (UserLevelHistory history : ordered) {
            if (!recordLevelChange(history.getUserId(), history.getOldLevel(), history.getNewLevel(),
                    history.getChangeReason(), history.getChangeType(), history.getOperatorId(),
                    history.getIpAddress(), history.getUserAgent())) throw new WeebException("等级变更保存失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean deleteUserHistory(Long userId) {
        return userLevelHistoryMapper.deleteByUserId(userId) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 1) throw new WeebException("无效的记录状态");
        return userLevelHistoryMapper.updateStatus(id, status) == 1;
    }

    private User requireUser(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) throw new WeebException("用户不存在");
        return user;
    }

    private String bounded(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
