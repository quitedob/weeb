package com.web.service;

import com.web.model.SystemConfig;
import com.web.model.SystemConfigHistory;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 * 提供配置的增删改查、历史记录、导入导出等功能
 */
public interface ConfigService {

    /**
     * 获取配置值（根据配置键）
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 获取配置值（带默认值）
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 获取配置值（指定类型）
     * @param configKey 配置键
     * @param type 期望的类型
     * @param <T> 类型参数
     * @return 指定类型的配置值
     */
    <T> T getConfigValue(String configKey, Class<T> type);

    /**
     * 获取配置值（指定类型，带默认值）
     * @param configKey 配置键
     * @param type 期望的类型
     * @param defaultValue 默认值
     * @param <T> 类型参数
     * @return 指定类型的配置值
     */
    <T> T getConfigValue(String configKey, Class<T> type, T defaultValue);

    /**
     * 获取配置详情
     * @param configKey 配置键
     * @return 配置对象
     */
    SystemConfig getConfig(String configKey);

    /**
     * 获取配置详情（根据ID）
     * @param configId 配置ID
     * @return 配置对象
     */
    SystemConfig getConfigById(Long configId);

    /**
     * 获取所有配置
     * @param group 配置分组（可选）
     * @return 配置列表
     */
    Map<String, Object> getAllConfigs(String group);

    /**
     * 分页获取配置列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param configKey 配置键过滤
     * @param configGroup 配置分组过滤
     * @param configCategory 配置分类过滤
     * @param configType 配置类型过滤
     * @param isEnabled 是否启用过滤
     * @param isSystem 是否系统配置过滤
     * @param keyword 关键词搜索
     * @return 分页结果
     */
    Map<String, Object> getConfigsWithPaging(int page, int pageSize,
                                             String configKey, String configGroup,
                                             String configCategory, String configType,
                                             Boolean isEnabled, Boolean isSystem,
                                             String keyword);

    /**
     * 创建或更新配置
     * @param config 配置对象
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    boolean saveConfig(SystemConfig config, Long operatorId);

    /**
     * 批量保存配置
     * @param configs 配置列表
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    Map<String, Object> batchSaveConfigs(List<SystemConfig> configs, Long operatorId);

    /**
     * 删除配置
     * @param configId 配置ID
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    boolean deleteConfig(Long configId, Long operatorId);

    /**
     * 批量删除配置
     * @param configIds 配置ID列表
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    Map<String, Object> batchDeleteConfigs(List<Long> configIds, Long operatorId);

    /**
     * 启用/禁用配置
     * @param configId 配置ID
     * @param enabled 是否启用
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    boolean toggleConfig(Long configId, boolean enabled, Long operatorId);

    /**
     * 重置配置为默认值
     * @param configId 配置ID
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    boolean resetConfigToDefault(Long configId, Long operatorId);

    /**
     * 验证配置值
     * @param config 配置对象
     * @return 验证结果
     */
    Map<String, Object> validateConfig(SystemConfig config);

    /**
     * 获取配置分组列表
     * @return 分组列表
     */
    List<String> getConfigGroups();

    /**
     * 获取配置分类列表
     * @return 分类列表
     */
    List<String> getConfigCategories();

    /**
     * 获取配置类型列表
     * @return 类型列表
     */
    List<String> getConfigTypes();

    /**
     * 根据分组获取配置
     * @param configGroup 配置分组
     * @return 配置列表
     */
    List<SystemConfig> getConfigsByGroup(String configGroup);

    /**
     * 搜索配置
     * @param keyword 关键词
     * @param limit 返回数量限制
     * @return 搜索结果
     */
    List<SystemConfig> searchConfigs(String keyword, int limit);

    /**
     * 导出配置
     * @param format 导出格式 (json, yaml, properties)
     * @param configGroup 配置分组（可选）
     * @param includeSensitive 是否包含敏感配置
     * @return 导出数据
     */
    byte[] exportConfigs(String format, String configGroup, boolean includeSensitive);

    /**
     * 导入配置
     * @param configData 配置数据
     * @param format 数据格式
     * @param operatorId 操作者ID
     * @param overwrite 是否覆盖现有配置
     * @return 导入结果
     */
    Map<String, Object> importConfigs(byte[] configData, String format, Long operatorId, boolean overwrite);

    /**
     * 获取配置变更历史
     * @param page 页码
     * @param pageSize 每页大小
     * @param configId 配置ID（可选）
     * @param configKey 配置键（可选）
     * @param actionType 操作类型（可选）
     * @param operatorId 操作者ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 历史记录列表
     */
    Map<String, Object> getConfigHistory(int page, int pageSize,
                                         Long configId, String configKey,
                                         String actionType, Long operatorId,
                                         String startDate, String endDate);

    /**
     * 回滚配置到指定历史版本
     * @param historyId 历史记录ID
     * @param operatorId 操作者ID
     * @return 回滚结果
     */
    boolean rollbackConfig(Long historyId, Long operatorId);

    /**
     * 获取配置统计信息
     * @return 统计信息
     */
    Map<String, Object> getConfigStatistics();

    /**
     * 获取配置变更统计
     * @param days 统计天数
     * @return 变更统计
     */
    Map<String, Object> getConfigChangeStatistics(int days);

    /**
     * 刷新配置缓存
     * @param configKey 配置键（可选，为空则刷新所有）
     */
    void refreshConfigCache(String configKey);

    /**
     * 检查配置依赖关系
     * @param configKey 配置键
     * @return 依赖的配置列表
     */
    List<String> checkConfigDependencies(String configKey);

    /**
     * 获取配置影响范围
     * @param configKey 配置键
     * @return 影响的功能模块列表
     */
    List<String> getConfigImpactScope(String configKey);

    /**
     * 验证配置完整性
     * @return 验证结果
     */
    Map<String, Object> validateConfigIntegrity();

    /**
     * 备份配置
     * @param backupType 备份类型
     * @param operatorId 操作者ID
     * @return 备份结果
     */
    Map<String, Object> backupConfigs(String backupType, Long operatorId);

    /**
     * 恢复配置
     * @param backupPath 备份文件路径
     * @param operatorId 操作者ID
     * @return 恢复结果
     */
    Map<String, Object> restoreConfigs(String backupPath, Long operatorId);

    /**
     * 清理配置历史记录
     * @param retentionDays 保留天数
     * @return 清理结果
     */
    Map<String, Object> cleanupConfigHistory(int retentionDays);

    /**
     * 获取系统状态摘要
     * @return 系统状态
     */
    Map<String, Object> getSystemStatusSummary();

    /**
     * 同步配置到数据库
     * 将缓存中的配置同步到数据库
     * @return 同步结果
     */
    Map<String, Object> syncConfigsToDatabase();

    /**
     * 重新加载配置
     * 从数据库重新加载配置到缓存
     * @return 重新加载结果
     */
    Map<String, Object> reloadConfigs();
}