package com.web.mapper;

import com.web.model.SystemConfig;
import com.web.model.SystemConfigHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 系统配置数据访问层
 */
@Mapper
public interface SystemConfigMapper {

    /**
     * 插入新配置
     */
    int insertConfig(SystemConfig config);

    /**
     * 更新配置
     */
    int updateConfig(SystemConfig config);

    /**
     * 根据ID查询配置
     */
    SystemConfig findById(@Param("id") Long id);

    /**
     * 根据配置键查询配置
     */
    SystemConfig findByKey(@Param("configKey") String configKey);

    /**
     * 根据配置键查询配置值
     */
    String findValueByKey(@Param("configKey") String configKey);

    /**
     * 分页查询配置列表
     */
    List<SystemConfig> findConfigsWithPaging(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计配置总数
     */
    long countConfigs();

    /**
     * 根据条件查询配置列表
     */
    List<SystemConfig> findConfigsWithFilters(
        @Param("offset") int offset,
        @Param("pageSize") int pageSize,
        @Param("configKey") String configKey,
        @Param("configGroup") String configGroup,
        @Param("configCategory") String configCategory,
        @Param("configType") String configType,
        @Param("isEnabled") Boolean isEnabled,
        @Param("isSystem") Boolean isSystem,
        @Param("keyword") String keyword
    );

    /**
     * 根据条件统计配置数量
     */
    long countConfigsWithFilters(
        @Param("configKey") String configKey,
        @Param("configGroup") String configGroup,
        @Param("configCategory") String configCategory,
        @Param("configType") String configType,
        @Param("isEnabled") Boolean isEnabled,
        @Param("isSystem") Boolean isSystem,
        @Param("keyword") String keyword
    );

    /**
     * 根据分组查询配置
     */
    List<SystemConfig> findByGroup(@Param("configGroup") String configGroup);

    /**
     * 根据分组查询启用的配置
     */
    List<SystemConfig> findEnabledByGroup(@Param("configGroup") String configGroup);

    /**
     * 根据分类查询配置
     */
    List<SystemConfig> findByCategory(@Param("configCategory") String configCategory);

    /**
     * 获取所有配置分组
     */
    List<String> getDistinctGroups();

    /**
     * 获取所有配置分类
     */
    List<String> getDistinctCategories();

    /**
     * 获取所有配置类型
     */
    List<String> getDistinctTypes();

    /**
     * 删除配置
     */
    int deleteConfig(@Param("id") Long id);

    /**
     * 批量删除配置
     */
    int batchDeleteConfigs(@Param("configIds") List<Long> configIds);

    /**
     * 批量更新配置
     */
    int batchUpdateConfigs(@Param("configs") List<SystemConfig> configs);

    /**
     * 搜索配置
     */
    List<SystemConfig> searchConfigs(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 检查配置键是否存在
     */
    boolean existsByKey(@Param("configKey") String configKey);

    /**
     * 检查配置键是否存在（排除指定ID）
     */
    boolean existsByKeyExcludingId(@Param("configKey") String configKey, @Param("excludeId") Long excludeId);

    /**
     * 获取系统核心配置
     */
    List<SystemConfig> findSystemConfigs();

    /**
     * 获取敏感配置
     */
    List<SystemConfig> findSensitiveConfigs();

    /**
     * 获取需要重启的配置
     */
    List<SystemConfig> findConfigsRequiringRestart();

    /**
     * 根据标签查询配置
     */
    List<SystemConfig> findByTag(@Param("tag") String tag);

    /**
     * 获取配置统计信息
     */
    Map<String, Object> getConfigStatistics();

    /**
     * 获取最近修改的配置
     */
    List<SystemConfig> getRecentlyModified(@Param("limit") int limit);

    /**
     * 更新配置版本号
     */
    int updateVersion(@Param("id") Long id, @Param("version") Integer version);

    /**
     * 配置历史记录相关方法
     */

    /**
     * 插入配置变更历史
     */
    int insertConfigHistory(SystemConfigHistory history);

    /**
     * 分页查询配置变更历史
     */
    List<SystemConfigHistory> findHistoryWithPaging(
        @Param("offset") int offset,
        @Param("pageSize") int pageSize,
        @Param("configId") Long configId,
        @Param("configKey") String configKey,
        @Param("actionType") String actionType,
        @Param("operatorId") Long operatorId,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );

    /**
     * 统计配置变更历史数量
     */
    long countHistoryWithFilters(
        @Param("configId") Long configId,
        @Param("configKey") String configKey,
        @Param("actionType") String actionType,
        @Param("operatorId") Long operatorId,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );

    /**
     * 根据配置ID查询变更历史
     */
    List<SystemConfigHistory> findHistoryByConfigId(@Param("configId") Long configId, @Param("limit") int limit);

    /**
     * 根据批次ID查询变更历史
     */
    List<SystemConfigHistory> findHistoryByBatchId(@Param("batchId") String batchId);

    /**
     * 获取最近的配置变更
     */
    List<SystemConfigHistory> getRecentHistory(@Param("limit") int limit);

    /**
     * 删除指定日期之前的历史记录
     */
    int deleteHistoryBefore(@Param("cutoffDate") String cutoffDate);

    /**
     * 获取配置变更统计
     */
    Map<String, Object> getHistoryStatistics(@Param("days") int days);

    /**
     * 清理配置历史记录
     */
    int cleanupHistory(@Param("retentionDays") int retentionDays);
}