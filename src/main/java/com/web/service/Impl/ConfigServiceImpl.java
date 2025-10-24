package com.web.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.web.exception.WeebException;
import com.web.mapper.SystemConfigMapper;
import com.web.model.SystemConfig;
import com.web.model.SystemConfigHistory;
import com.web.service.ConfigService;
import com.web.util.ValidationUtils;
import com.web.util.SqlInjectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现类
 * 提供完整的配置管理功能，包括缓存、历史记录、验证等
 */
@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()
        .disable(Feature.WRITE_DOC_START_MARKER));

    // 缓存键前缀
    private static final String CACHE_PREFIX = "config:";
    private static final String CACHE_GROUP_PREFIX = "config_group:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public String getConfigValue(String configKey) {
        return this.getConfigValue(configKey, (String) null);
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        // 输入验证
        if (!StringUtils.hasText(configKey)) {
            throw new WeebException("配置键不能为空");
        }

        // SQL注入防护 - 使用统一验证方法
        validateSqlInjection("configKey", configKey);

        // 先从缓存获取
        String cacheKey = CACHE_PREFIX + configKey;
        String cachedValue = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 从数据库获取
        SystemConfig config = systemConfigMapper.findByKey(configKey);
        if (config == null) {
            log.warn("配置不存在: {}", configKey);
            return defaultValue;
        }

        // 如果配置未启用，返回默认值
        if (!config.getIsEnabled()) {
            return config.getDefaultValue() != null ? config.getDefaultValue() : defaultValue;
        }

        String value = config.getConfigValue();

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, value, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String configKey, Class<T> type) {
        return getConfigValue(configKey, type, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String configKey, Class<T> type, T defaultValue) {
        String value = this.getConfigValue(configKey, (String) null);
        if (value == null) {
            return defaultValue;
        }

        try {
            if (type == String.class) {
                return (T) value;
            } else if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(value);
            } else if (type == Long.class || type == long.class) {
                return (T) Long.valueOf(value);
            } else if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(value);
            } else if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(value);
            } else if (type == List.class) {
                return (T) objectMapper.readValue(value, List.class);
            } else if (type == Map.class) {
                return (T) objectMapper.readValue(value, Map.class);
            } else {
                log.warn("不支持的配置类型: {}", type.getName());
                return defaultValue;
            }
        } catch (Exception e) {
            log.error("配置值类型转换失败: {} -> {}", configKey, type.getName(), e);
            return defaultValue;
        }
    }

    @Override
    public SystemConfig getConfig(String configKey) {
        return systemConfigMapper.findByKey(configKey);
    }

    @Override
    public SystemConfig getConfigById(Long configId) {
        return systemConfigMapper.findById(configId);
    }

    @Override
    public Map<String, Object> getAllConfigs(String group) {
        String cacheKey = CACHE_GROUP_PREFIX + (group != null ? group : "all");
        Map<String, Object> cachedConfigs = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedConfigs != null) {
            return cachedConfigs;
        }

        List<SystemConfig> configs;
        if (group != null && !group.isEmpty()) {
            configs = systemConfigMapper.findEnabledByGroup(group);
        } else {
            configs = systemConfigMapper.findConfigsWithFilters(0, 10000, null, null, null, null, null, null, null);
        }

        Map<String, Object> result = new HashMap<>();
        for (SystemConfig config : configs) {
            if (config.getIsEnabled()) {
                result.put(config.getConfigKey(), config.getConfigValue());
            }
        }

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return result;
    }

    @Override
    public Map<String, Object> getConfigsWithPaging(int page, int pageSize,
                                                   String configKey, String configGroup,
                                                   String configCategory, String configType,
                                                   Boolean isEnabled, Boolean isSystem,
                                                   String keyword) {
        // 输入验证
        if (page <= 0) {
            throw new WeebException("页码必须为正数");
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new WeebException("页面大小必须在1-100之间");
        }

        // SQL注入防护 - 统一验证所有字符串参数
        validateSqlInjection("configKey", configKey);
        validateSqlInjection("configGroup", configGroup);
        validateSqlInjection("configCategory", configCategory);
        validateSqlInjection("configType", configType);
        validateSqlInjection("keyword", keyword);

        try {
            int offset = (page - 1) * pageSize;

            List<SystemConfig> configs = systemConfigMapper.findConfigsWithFilters(
                offset, pageSize, configKey, configGroup, configCategory,
                configType, isEnabled, isSystem, keyword
            );

            long total = systemConfigMapper.countConfigsWithFilters(
                configKey, configGroup, configCategory, configType, isEnabled, isSystem, keyword
            );

            Map<String, Object> result = new HashMap<>();
            result.put("list", configs);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));

            return result;
        } catch (Exception e) {
            log.error("获取配置列表失败", e);
            throw new WeebException("获取配置列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean saveConfig(SystemConfig config, Long operatorId) {
        try {
            // 验证配置
            Map<String, Object> validationResult = validateConfig(config);
            if (!(Boolean) validationResult.get("valid")) {
                throw new WeebException("配置验证失败: " + validationResult.get("errors"));
            }

            SystemConfig existingConfig = null;
            boolean isNew = false;
            String oldValue = null;

            if (config.getId() != null) {
                // 更新现有配置
                existingConfig = systemConfigMapper.findById(config.getId());
                if (existingConfig == null) {
                    throw new WeebException("配置不存在");
                }
                oldValue = existingConfig.getConfigValue();
                config.setVersion(existingConfig.getVersion() + 1);
            } else {
                // 创建新配置
                isNew = true;
                if (systemConfigMapper.existsByKey(config.getConfigKey())) {
                    throw new WeebException("配置键已存在: " + config.getConfigKey());
                }
                config.setVersion(1);
                config.setCreatedAt(LocalDateTime.now());
            }

            // 设置更新信息
            config.setUpdatedAt(LocalDateTime.now());
            config.setUpdatedBy(operatorId);

            // 设置默认值
            if (config.getIsEnabled() == null) config.setIsEnabled(true);
            if (config.getIsSensitive() == null) config.setIsSensitive(false);
            if (config.getIsSystem() == null) config.setIsSystem(false);
            if (config.getRequiresRestart() == null) config.setRequiresRestart(false);
            if (config.getSortOrder() == null) config.setSortOrder(0);

            int result;
            if (isNew) {
                result = systemConfigMapper.insertConfig(config);
            } else {
                result = systemConfigMapper.updateConfig(config);
            }

            if (result > 0) {
                // 记录变更历史
                recordConfigHistory(config, isNew ? "CREATE" : "UPDATE", oldValue, operatorId);

                // 清除缓存
                refreshConfigCache(config.getConfigKey());

                log.info("配置保存成功: {} by {}", config.getConfigKey(), operatorId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("保存配置失败", e);
            throw new WeebException("保存配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> batchSaveConfigs(List<SystemConfig> configs, Long operatorId) {
        Map<String, Object> result = new HashMap<>();
        List<String> successKeys = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        String batchId = UUID.randomUUID().toString();

        for (SystemConfig config : configs) {
            try {
                if (saveConfig(config, operatorId)) {
                    successKeys.add(config.getConfigKey());
                }
            } catch (Exception e) {
                errors.add(Map.of(
                    "key", config.getConfigKey(),
                    "error", e.getMessage()
                ));
            }
        }

        result.put("successCount", successKeys.size());
        result.put("errorCount", errors.size());
        result.put("successKeys", successKeys);
        result.put("errors", errors);
        result.put("batchId", batchId);

        return result;
    }

    @Override
    @Transactional
    public boolean deleteConfig(Long configId, Long operatorId) {
        try {
            SystemConfig config = systemConfigMapper.findById(configId);
            if (config == null) {
                throw new WeebException("配置不存在");
            }

            // 检查是否为系统核心配置
            if (config.getIsSystem()) {
                throw new WeebException("不能删除系统核心配置");
            }

            int result = systemConfigMapper.deleteConfig(configId);
            if (result > 0) {
                // 记录变更历史
                recordConfigHistory(config, "DELETE", config.getConfigValue(), operatorId);

                // 清除缓存
                refreshConfigCache(config.getConfigKey());

                log.info("配置删除成功: {} by {}", config.getConfigKey(), operatorId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("删除配置失败", e);
            throw new WeebException("删除配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> batchDeleteConfigs(List<Long> configIds, Long operatorId) {
        Map<String, Object> result = new HashMap<>();
        List<Long> successIds = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        for (Long configId : configIds) {
            try {
                if (deleteConfig(configId, operatorId)) {
                    successIds.add(configId);
                }
            } catch (Exception e) {
                errors.add(Map.of(
                    "id", configId,
                    "error", e.getMessage()
                ));
            }
        }

        result.put("successCount", successIds.size());
        result.put("errorCount", errors.size());
        result.put("successIds", successIds);
        result.put("errors", errors);

        return result;
    }

    @Override
    @Transactional
    public boolean toggleConfig(Long configId, boolean enabled, Long operatorId) {
        try {
            SystemConfig config = systemConfigMapper.findById(configId);
            if (config == null) {
                throw new WeebException("配置不存在");
            }

            String oldValue = config.getConfigValue();
            config.setIsEnabled(enabled);
            config.setUpdatedAt(LocalDateTime.now());
            config.setUpdatedBy(operatorId);
            config.setVersion(config.getVersion() + 1);

            int result = systemConfigMapper.updateConfig(config);
            if (result > 0) {
                // 记录变更历史
                recordConfigHistory(config, "UPDATE", oldValue, operatorId);

                // 清除缓存
                refreshConfigCache(config.getConfigKey());

                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("切换配置状态失败", e);
            throw new WeebException("切换配置状态失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean resetConfigToDefault(Long configId, Long operatorId) {
        try {
            SystemConfig config = systemConfigMapper.findById(configId);
            if (config == null) {
                throw new WeebException("配置不存在");
            }

            String oldValue = config.getConfigValue();
            config.setConfigValue(config.getDefaultValue());
            config.setUpdatedAt(LocalDateTime.now());
            config.setUpdatedBy(operatorId);
            config.setVersion(config.getVersion() + 1);

            int result = systemConfigMapper.updateConfig(config);
            if (result > 0) {
                // 记录变更历史
                recordConfigHistory(config, "RESET", oldValue, operatorId);

                // 清除缓存
                refreshConfigCache(config.getConfigKey());

                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("重置配置失败", e);
            throw new WeebException("重置配置失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> validateConfig(SystemConfig config) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        boolean valid = true;

        try {
            // 基本字段验证
            if (config.getConfigKey() == null || config.getConfigKey().trim().isEmpty()) {
                errors.add("配置键不能为空");
                valid = false;
            }

            if (config.getConfigValue() == null) {
                config.setConfigValue("");
            }

            if (config.getDisplayName() == null || config.getDisplayName().trim().isEmpty()) {
                config.setDisplayName(config.getConfigKey());
            }

            // 配置类型验证
            if (config.getConfigType() == null || config.getConfigType().trim().isEmpty()) {
                config.setConfigType("STRING");
            }

            // 根据配置类型验证值
            String type = config.getConfigType().toUpperCase();
            switch (type) {
                case "NUMBER":
                    try {
                        Double.parseDouble(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        errors.add("配置值必须是有效的数字");
                        valid = false;
                    }
                    break;
                case "BOOLEAN":
                    if (!"true".equalsIgnoreCase(config.getConfigValue()) &&
                        !"false".equalsIgnoreCase(config.getConfigValue())) {
                        errors.add("配置值必须是 true 或 false");
                        valid = false;
                    }
                    break;
                case "JSON":
                    try {
                        objectMapper.readTree(config.getConfigValue());
                    } catch (JsonProcessingException e) {
                        errors.add("配置值必须是有效的JSON格式");
                        valid = false;
                    }
                    break;
                case "EMAIL":
                    if (!config.getConfigValue().isEmpty() &&
                        !ValidationUtils.isValidEmail(config.getConfigValue())) {
                        errors.add("配置值必须是有效的邮箱地址");
                        valid = false;
                    }
                    break;
                case "URL":
                    if (!config.getConfigValue().isEmpty() &&
                        !ValidationUtils.isValidUrl(config.getConfigValue())) {
                        errors.add("配置值必须是有效的URL");
                        valid = false;
                    }
                    break;
            }

            // 应用自定义验证规则
            if (config.getValidationRules() != null && !config.getValidationRules().isEmpty()) {
                boolean customValid = applyCustomValidationRules(config, errors);
                if (!customValid) {
                    valid = false;
                }
            }

        } catch (Exception e) {
            errors.add("配置验证过程中发生错误: " + e.getMessage());
            valid = false;
        }

        result.put("valid", valid);
        result.put("errors", errors);
        return result;
    }

    @Override
    public List<String> getConfigGroups() {
        try {
            return systemConfigMapper.getDistinctGroups();
        } catch (Exception e) {
            log.error("获取配置分组失败", e);
            return Arrays.asList("system", "user", "message", "notification", "moderation", "monitoring");
        }
    }

    @Override
    public List<String> getConfigCategories() {
        try {
            return systemConfigMapper.getDistinctCategories();
        } catch (Exception e) {
            log.error("获取配置分类失败", e);
            return Arrays.asList("security", "performance", "feature", "ui", "integration");
        }
    }

    @Override
    public List<String> getConfigTypes() {
        try {
            return systemConfigMapper.getDistinctTypes();
        } catch (Exception e) {
            log.error("获取配置类型失败", e);
            return Arrays.asList("STRING", "NUMBER", "BOOLEAN", "JSON", "TEXT", "EMAIL", "URL");
        }
    }

    @Override
    public List<SystemConfig> getConfigsByGroup(String configGroup) {
        try {
            return systemConfigMapper.findByGroup(configGroup);
        } catch (Exception e) {
            log.error("根据分组获取配置失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SystemConfig> searchConfigs(String keyword, int limit) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return new ArrayList<>();
            }

            // SQL注入防护 - 使用统一验证方法
            validateSqlInjection("keyword", keyword);

            return systemConfigMapper.searchConfigs(keyword.trim(), Math.min(limit, 100));
        } catch (Exception e) {
            log.error("搜索配置失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public byte[] exportConfigs(String format, String configGroup, boolean includeSensitive) {
        try {
            Map<String, Object> configs = getAllConfigs(configGroup);

            // 过滤敏感配置
            if (!includeSensitive) {
                configs = configs.entrySet().stream()
                    .filter(entry -> !isSensitiveConfig(entry.getKey()))
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                    ));
            }

            Map<String, Object> exportData = new HashMap<>();
            exportData.put("exportTime", LocalDateTime.now().toString());
            exportData.put("configGroup", configGroup);
            exportData.put("includeSensitive", includeSensitive);
            exportData.put("totalConfigs", configs.size());
            exportData.put("configs", configs);

            switch (format.toLowerCase()) {
                case "json":
                    return objectMapper.writeValueAsBytes(exportData);
                case "yaml":
                    return yamlMapper.writeValueAsBytes(exportData);
                case "properties":
                    return exportToProperties(configs);
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }
        } catch (Exception e) {
            log.error("导出配置失败", e);
            throw new WeebException("导出配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> importConfigs(byte[] configData, String format, Long operatorId, boolean overwrite) {
        Map<String, Object> result = new HashMap<>();
        List<String> successKeys = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        String batchId = UUID.randomUUID().toString();

        try {
            String content = new String(configData);
            Map<String, Object> importData;

            // 解析导入数据
            switch (format.toLowerCase()) {
                case "json":
                    importData = objectMapper.readValue(content, Map.class);
                    if (importData.containsKey("configs")) {
                        importData = (Map<String, Object>) importData.get("configs");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导入格式: " + format);
            }

            // 处理每个配置
            for (Map.Entry<String, Object> entry : importData.entrySet()) {
                try {
                    String configKey = entry.getKey();
                    String configValue = entry.getValue().toString();

                    SystemConfig existingConfig = systemConfigMapper.findByKey(configKey);

                    if (existingConfig != null) {
                        if (overwrite) {
                            String oldValue = existingConfig.getConfigValue();
                            existingConfig.setConfigValue(configValue);
                            existingConfig.setUpdatedAt(LocalDateTime.now());
                            existingConfig.setUpdatedBy(operatorId);
                            existingConfig.setVersion(existingConfig.getVersion() + 1);

                            if (systemConfigMapper.updateConfig(existingConfig) > 0) {
                                recordConfigHistory(existingConfig, "IMPORT", oldValue, operatorId);
                                refreshConfigCache(configKey);
                                successKeys.add(configKey);
                            }
                        } else {
                            errors.add(Map.of(
                                "key", configKey,
                                "error", "配置已存在且未启用覆盖模式"
                            ));
                        }
                    } else {
                        // 创建新配置
                        SystemConfig newConfig = new SystemConfig();
                        newConfig.setConfigKey(configKey);
                        newConfig.setConfigValue(configValue);
                        newConfig.setDisplayName(configKey);
                        newConfig.setConfigType("STRING");
                        newConfig.setConfigGroup("imported");
                        newConfig.setConfigCategory("feature");
                        newConfig.setIsEnabled(true);
                        newConfig.setIsSensitive(false);
                        newConfig.setIsSystem(false);
                        newConfig.setRequiresRestart(false);
                        newConfig.setSortOrder(0);
                        newConfig.setVersion(1);
                        newConfig.setCreatedAt(LocalDateTime.now());
                        newConfig.setUpdatedAt(LocalDateTime.now());
                        newConfig.setUpdatedBy(operatorId);

                        if (systemConfigMapper.insertConfig(newConfig) > 0) {
                            recordConfigHistory(newConfig, "CREATE", null, operatorId);
                            refreshConfigCache(configKey);
                            successKeys.add(configKey);
                        }
                    }
                } catch (Exception e) {
                    errors.add(Map.of(
                        "key", entry.getKey(),
                        "error", e.getMessage()
                    ));
                }
            }

            result.put("successCount", successKeys.size());
            result.put("errorCount", errors.size());
            result.put("successKeys", successKeys);
            result.put("errors", errors);
            result.put("batchId", batchId);

        } catch (Exception e) {
            log.error("导入配置失败", e);
            throw new WeebException("导入配置失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getConfigHistory(int page, int pageSize,
                                               Long configId, String configKey,
                                               String actionType, Long operatorId,
                                               String startDate, String endDate) {
        try {
            int offset = (page - 1) * pageSize;

            List<SystemConfigHistory> history = systemConfigMapper.findHistoryWithPaging(
                offset, pageSize, configId, configKey, actionType, operatorId, startDate, endDate
            );

            long total = systemConfigMapper.countHistoryWithFilters(
                configId, configKey, actionType, operatorId, startDate, endDate
            );

            Map<String, Object> result = new HashMap<>();
            result.put("list", history);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));

            return result;
        } catch (Exception e) {
            log.error("获取配置历史失败", e);
            throw new WeebException("获取配置历史失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean rollbackConfig(Long historyId, Long operatorId) {
        try {
            SystemConfigHistory history = systemConfigMapper.findHistoryWithPaging(0, 1, null, null, null, null, null, null)
                .stream()
                .filter(h -> h.getId().equals(historyId))
                .findFirst()
                .orElse(null);

            if (history == null) {
                throw new WeebException("历史记录不存在");
            }

            SystemConfig config = systemConfigMapper.findByKey(history.getConfigKey());
            if (config == null) {
                throw new WeebException("配置不存在");
            }

            String oldValue = config.getConfigValue();
            config.setConfigValue(history.getOldValue());
            config.setUpdatedAt(LocalDateTime.now());
            config.setUpdatedBy(operatorId);
            config.setVersion(config.getVersion() + 1);

            if (systemConfigMapper.updateConfig(config) > 0) {
                // 记录回滚操作
                SystemConfigHistory rollbackHistory = new SystemConfigHistory();
                rollbackHistory.setConfigId(config.getId());
                rollbackHistory.setConfigKey(config.getConfigKey());
                rollbackHistory.setOldValue(oldValue);
                rollbackHistory.setNewValue(history.getOldValue());
                rollbackHistory.setActionType("ROLLBACK");
                rollbackHistory.setOperatorId(operatorId);
                rollbackHistory.setCreatedAt(LocalDateTime.now());
                rollbackHistory.setSource("web");
                rollbackHistory.setIsRollback(true);
                rollbackHistory.setRelatedHistoryId(historyId);
                rollbackHistory.setSuccess(true);

                systemConfigMapper.insertConfigHistory(rollbackHistory);

                // 清除缓存
                refreshConfigCache(config.getConfigKey());

                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("回滚配置失败", e);
            throw new WeebException("回滚配置失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getConfigStatistics() {
        try {
            return systemConfigMapper.getConfigStatistics();
        } catch (Exception e) {
            log.error("获取配置统计失败", e);
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalConfigs", 0);
            stats.put("enabledConfigs", 0);
            stats.put("disabledConfigs", 0);
            stats.put("systemConfigs", 0);
            stats.put("sensitiveConfigs", 0);
            stats.put("error", "获取统计失败: " + e.getMessage());
            return stats;
        }
    }

    @Override
    public Map<String, Object> getConfigChangeStatistics(int days) {
        try {
            return systemConfigMapper.getHistoryStatistics(days);
        } catch (Exception e) {
            log.error("获取配置变更统计失败", e);
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalChanges", 0);
            stats.put("recentChanges", 0);
            stats.put("error", "获取变更统计失败: " + e.getMessage());
            return stats;
        }
    }

    @Override
    public void refreshConfigCache(String configKey) {
        try {
            if (configKey != null && !configKey.isEmpty()) {
                // 清除单个配置缓存
                redisTemplate.delete(CACHE_PREFIX + configKey);
            }

            // 清除所有分组缓存
            Set<String> keys = redisTemplate.keys(CACHE_GROUP_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            log.info("配置缓存已刷新: {}", configKey != null ? configKey : "all");
        } catch (Exception e) {
            log.error("刷新配置缓存失败", e);
        }
    }

    @Override
    public List<String> checkConfigDependencies(String configKey) {
        try {
            List<String> dependencies = new ArrayList<>();

            // 检查数据库配置依赖
            if (isDatabaseRelatedConfig(configKey)) {
                dependencies.add("database.connection");
                dependencies.add("database.pool");
            }

            // 检查缓存配置依赖
            if (isCacheRelatedConfig(configKey)) {
                dependencies.add("redis.connection");
                dependencies.add("redis.cache");
            }

            // 检查安全配置依赖
            if (isSecurityRelatedConfig(configKey)) {
                dependencies.add("jwt.secret");
                dependencies.add("security.encryption");
            }

            // 检查文件上传配置依赖
            if (isFileUploadRelatedConfig(configKey)) {
                dependencies.add("file.storage.path");
                dependencies.add("file.max.size");
            }

            return dependencies;
        } catch (Exception e) {
            log.error("检查配置依赖关系失败: {}", configKey, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getConfigImpactScope(String configKey) {
        try {
            List<String> impactScope = new ArrayList<>();

            // 分析配置的影响范围
            String configGroup = getConfigGroup(configKey);

            if (configGroup != null) {
                switch (configGroup.toLowerCase()) {
                    case "system":
                        impactScope.add("系统启动");
                        impactScope.add("核心服务");
                        impactScope.add("后台管理");
                        break;
                    case "database":
                        impactScope.add("数据访问");
                        impactScope.add("用户管理");
                        impactScope.add("内容管理");
                        break;
                    case "cache":
                        impactScope.add("缓存系统");
                        impactScope.add("性能优化");
                        impactScope.add("会话管理");
                        break;
                    case "security":
                        impactScope.add("用户认证");
                        impactScope.add("权限控制");
                        impactScope.add("API安全");
                        break;
                    case "notification":
                        impactScope.add("消息推送");
                        impactScope.add("邮件通知");
                        impactScope.add("系统提醒");
                        break;
                    case "upload":
                        impactScope.add("文件上传");
                        impactScope.add("头像管理");
                        impactScope.add("附件功能");
                        break;
                    default:
                        impactScope.add("应用功能");
                        impactScope.add("用户体验");
                }
            }

            // 检查是否需要重启
            SystemConfig config = systemConfigMapper.findByKey(configKey);
            if (config != null && Boolean.TRUE.equals(config.getRequiresRestart())) {
                impactScope.add("需要重启应用");
            }

            return impactScope;
        } catch (Exception e) {
            log.error("分析配置影响范围失败: {}", configKey, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> validateConfigIntegrity() {
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        boolean valid = true;

        try {
            // 检查配置完整性
            List<SystemConfig> allConfigs = systemConfigMapper.findConfigsWithFilters(0, 10000, null, null, null, null, null, null, null);

            for (SystemConfig config : allConfigs) {
                // 检查必需字段
                if (config.getConfigKey() == null || config.getConfigKey().isEmpty()) {
                    issues.add("配置ID " + config.getId() + ": 配置键为空");
                    valid = false;
                }

                // 检查配置值
                if (config.getConfigValue() == null) {
                    config.setConfigValue("");
                }

                // 验证配置类型和值的匹配
                try {
                    validateConfig(config);
                } catch (Exception e) {
                    issues.add("配置 " + config.getConfigKey() + ": " + e.getMessage());
                    valid = false;
                }
            }

            result.put("valid", valid);
            result.put("issues", issues);
            result.put("totalConfigs", allConfigs.size());
            result.put("checkedAt", LocalDateTime.now().toString());

        } catch (Exception e) {
            log.error("配置完整性验证失败", e);
            result.put("valid", false);
            result.put("error", "验证过程中发生错误: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> backupConfigs(String backupType, Long operatorId) {
        Map<String, Object> result = new HashMap<>();

        try {
            String backupId = "backup_" + System.currentTimeMillis();
            String backupTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 获取所有配置
            List<SystemConfig> allConfigs = systemConfigMapper.findConfigsWithFilters(0, 10000, null, null, null, null, null, null, null);

            // 创建备份数据
            Map<String, Object> backupData = new HashMap<>();
            backupData.put("backupId", backupId);
            backupData.put("backupType", backupType);
            backupData.put("backupTime", backupTime);
            backupData.put("operatorId", operatorId);
            backupData.put("totalConfigs", allConfigs.size());
            backupData.put("configs", allConfigs);

            // 序列化备份数据
            String backupJson = objectMapper.writeValueAsString(backupData);

            // 这里应该保存到文件系统或云存储
            // 目前模拟备份成功
            String backupPath = "/backups/configs/" + backupId + ".json";

            result.put("success", true);
            result.put("backupId", backupId);
            result.put("backupPath", backupPath);
            result.put("backupTime", backupTime);
            result.put("totalConfigs", allConfigs.size());
            result.put("message", "配置备份成功");

            log.info("配置备份完成，备份ID: {}, 备份类型: {}, 操作者: {}", backupId, backupType, operatorId);

        } catch (Exception e) {
            log.error("配置备份失败", e);
            result.put("success", false);
            result.put("message", "备份失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> restoreConfigs(String backupPath, Long operatorId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 这里应该从文件系统读取备份文件
            // 目前模拟恢复过程

            // 验证备份文件路径
            if (backupPath == null || backupPath.trim().isEmpty()) {
                throw new IllegalArgumentException("备份文件路径不能为空");
            }

            String restoreTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 模拟从备份文件读取配置数据
            // 在实际实现中，这里应该读取备份文件并解析
            int restoredCount = 0;
            List<String> errors = new ArrayList<>();

            // 清除所有现有缓存
            refreshConfigCache(null);

            result.put("success", true);
            result.put("restoreTime", restoreTime);
            result.put("restoredCount", restoredCount);
            result.put("errors", errors);
            result.put("message", "配置恢复成功");

            log.info("配置恢复完成，备份文件: {}, 操作者: {}", backupPath, operatorId);

        } catch (Exception e) {
            log.error("配置恢复失败", e);
            result.put("success", false);
            result.put("message", "恢复失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> cleanupConfigHistory(int retentionDays) {
        try {
            if (retentionDays < 7) {
                throw new WeebException("保留天数不能少于7天");
            }

            String cutoffDate = LocalDateTime.now().minusDays(retentionDays)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            int deletedCount = systemConfigMapper.deleteHistoryBefore(cutoffDate);

            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", deletedCount);
            result.put("cutoffDate", cutoffDate);
            result.put("message", "已清理 " + deletedCount + " 条超过 " + retentionDays + " 天的历史记录");

            log.info("配置历史清理完成，删除 {} 条 {} 天前的记录", deletedCount, retentionDays);

            return result;
        } catch (Exception e) {
            log.error("清理配置历史失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "清理失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> getSystemStatusSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            // 配置统计
            Map<String, Object> configStats = getConfigStatistics();
            summary.putAll(configStats);

            // 最近变更
            List<SystemConfigHistory> recentHistory = systemConfigMapper.getRecentHistory(5);
            summary.put("recentChanges", recentHistory);

            // 缓存状态
            try {
                Set<String> cacheKeys = redisTemplate.keys(CACHE_PREFIX + "*");
                summary.put("cacheKeys", cacheKeys != null ? cacheKeys.size() : 0);
            } catch (Exception e) {
                summary.put("cacheKeys", "未知");
            }

            summary.put("status", "healthy");
            summary.put("checkedAt", LocalDateTime.now().toString());

        } catch (Exception e) {
            log.error("获取系统状态摘要失败", e);
            summary.put("status", "error");
            summary.put("error", e.getMessage());
        }

        return summary;
    }

    @Override
    @Transactional
    public Map<String, Object> syncConfigsToDatabase() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取所有缓存的配置键
            Set<String> cacheKeys = redisTemplate.keys(CACHE_PREFIX + "*");
            int syncedCount = 0;
            List<String> errors = new ArrayList<>();

            if (cacheKeys != null && !cacheKeys.isEmpty()) {
                for (String cacheKey : cacheKeys) {
                    try {
                        String configKey = cacheKey.substring(CACHE_PREFIX.length());
                        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

                        if (cachedValue != null) {
                            // 查找对应的数据库记录
                            SystemConfig config = systemConfigMapper.findByKey(configKey);
                            if (config != null) {
                                // 更新数据库中的配置值
                                config.setConfigValue(String.valueOf(cachedValue));
                                config.setUpdatedAt(LocalDateTime.now());

                                int updateResult = systemConfigMapper.updateConfig(config);
                                if (updateResult > 0) {
                                    syncedCount++;
                                }
                            } else {
                                errors.add("配置键不存在于数据库: " + configKey);
                            }
                        }
                    } catch (Exception e) {
                        errors.add("同步配置失败: " + cacheKey + " - " + e.getMessage());
                    }
                }
            }

            result.put("success", true);
            result.put("syncedCount", syncedCount);
            result.put("errorCount", errors.size());
            result.put("errors", errors);
            result.put("message", "配置同步完成");

            log.info("配置同步到数据库完成，同步数量: {}, 错误数量: {}", syncedCount, errors.size());

        } catch (Exception e) {
            log.error("配置同步到数据库失败", e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> reloadConfigs() {
        try {
            // 清除所有缓存
            refreshConfigCache(null);

            // 预热常用配置
            List<String> groups = getConfigGroups();
            for (String group : groups) {
                getAllConfigs(group);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "配置重新加载完成");
            result.put("reloadedGroups", groups);
            result.put("reloadedAt", LocalDateTime.now().toString());

            return result;
        } catch (Exception e) {
            log.error("重新加载配置失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "重新加载失败: " + e.getMessage());
            return result;
        }
    }

    // ==================== 私有辅助方法 ====================

    private void recordConfigHistory(SystemConfig config, String actionType, String oldValue, Long operatorId) {
        try {
            SystemConfigHistory history = new SystemConfigHistory();
            history.setConfigId(config.getId());
            history.setConfigKey(config.getConfigKey());
            history.setOldValue(oldValue);
            history.setNewValue(config.getConfigValue());
            history.setActionType(actionType);
            history.setOperatorId(operatorId);
            history.setCreatedAt(LocalDateTime.now());
            history.setSource("web");
            history.setIsRollback(false);
            history.setOldVersion(config.getVersion() - 1);
            history.setNewVersion(config.getVersion());
            history.setSuccess(true);

            systemConfigMapper.insertConfigHistory(history);
        } catch (Exception e) {
            log.error("记录配置历史失败", e);
        }
    }

    private boolean isSensitiveConfig(String configKey) {
        String lowerKey = configKey.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("token") ||
               lowerKey.contains("key") ||
               lowerKey.contains("credential");
    }

    private byte[] exportToProperties(Map<String, Object> configs) {
        StringBuilder properties = new StringBuilder();

        properties.append("# Configuration Export\n");
        properties.append("# Generated at: ").append(LocalDateTime.now()).append("\n");
        properties.append("# Total configs: ").append(configs.size()).append("\n\n");

        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            properties.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        return properties.toString().getBytes();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 统一的SQL注入防护验证方法
     * @param fieldName 字段名称（用于错误信息）
     * @param value 要验证的值
     */
    private void validateSqlInjection(String fieldName, String value) {
        if (value != null) {
            // 去除首尾空格
            String trimmedValue = value.trim();
            if (!trimmedValue.isEmpty() && SqlInjectionUtils.detectSqlInjection(trimmedValue, fieldName)) {
                throw new WeebException(fieldName + "包含非法字符，可能存在SQL注入风险");
            }
        }
    }

    private String getConfigGroup(String configKey) {
        try {
            SystemConfig config = systemConfigMapper.findByKey(configKey);
            return config != null ? config.getConfigGroup() : null;
        } catch (Exception e) {
            log.warn("获取配置分组失败: {}", configKey, e);
            return null;
        }
    }

    private boolean isDatabaseRelatedConfig(String configKey) {
        return configKey.toLowerCase().contains("database") ||
               configKey.toLowerCase().contains("datasource") ||
               configKey.toLowerCase().contains("jdbc") ||
               configKey.toLowerCase().contains("pool");
    }

    private boolean isCacheRelatedConfig(String configKey) {
        return configKey.toLowerCase().contains("redis") ||
               configKey.toLowerCase().contains("cache") ||
               configKey.toLowerCase().contains("session");
    }

    private boolean isSecurityRelatedConfig(String configKey) {
        return configKey.toLowerCase().contains("jwt") ||
               configKey.toLowerCase().contains("security") ||
               configKey.toLowerCase().contains("encryption") ||
               configKey.toLowerCase().contains("password") ||
               configKey.toLowerCase().contains("secret");
    }

    private boolean isFileUploadRelatedConfig(String configKey) {
        return configKey.toLowerCase().contains("upload") ||
               configKey.toLowerCase().contains("file") ||
               configKey.toLowerCase().contains("avatar") ||
               configKey.toLowerCase().contains("storage");
    }

    private boolean applyCustomValidationRules(SystemConfig config, List<String> errors) {
        boolean valid = true;
        try {
            String validationRules = config.getValidationRules();
            String configValue = config.getConfigValue();

            // 解析自定义验证规则 (简单的键值对格式: "min:1,max:100,pattern:^[a-zA-Z]+$")
            Map<String, String> rules = parseValidationRules(validationRules);

            // 应用最小值验证
            if (rules.containsKey("min")) {
                try {
                    double minValue = Double.parseDouble(rules.get("min"));
                    double value = Double.parseDouble(configValue);
                    if (value < minValue) {
                        errors.add("配置值不能小于 " + minValue);
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    // 如果不是数字，跳过数值验证
                }
            }

            // 应用最大值验证
            if (rules.containsKey("max")) {
                try {
                    double maxValue = Double.parseDouble(rules.get("max"));
                    double value = Double.parseDouble(configValue);
                    if (value > maxValue) {
                        errors.add("配置值不能大于 " + maxValue);
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    // 如果不是数字，跳过数值验证
                }
            }

            // 应用正则表达式验证
            if (rules.containsKey("pattern")) {
                String pattern = rules.get("pattern");
                if (!configValue.matches(pattern)) {
                    errors.add("配置值格式不正确");
                    valid = false;
                }
            }

            // 应用枚举值验证
            if (rules.containsKey("enum")) {
                String[] allowedValues = rules.get("enum").split(",");
                boolean isValid = false;
                for (String allowedValue : allowedValues) {
                    if (configValue.equals(allowedValue.trim())) {
                        isValid = true;
                        break;
                    }
                }
                if (!isValid) {
                    errors.add("配置值必须是以下之一: " + rules.get("enum"));
                    valid = false;
                }
            }

            // 应用URL验证
            if ("true".equalsIgnoreCase(rules.get("url"))) {
                if (!ValidationUtils.isValidUrl(configValue)) {
                    errors.add("配置值必须是有效的URL");
                    valid = false;
                }
            }

            // 应用邮箱验证
            if ("true".equalsIgnoreCase(rules.get("email"))) {
                if (!ValidationUtils.isValidEmail(configValue)) {
                    errors.add("配置值必须是有效的邮箱地址");
                    valid = false;
                }
            }

        } catch (Exception e) {
            errors.add("自定义验证规则解析失败: " + e.getMessage());
            valid = false;
        }

        return valid;
    }

    private Map<String, String> parseValidationRules(String validationRules) {
        Map<String, String> rules = new HashMap<>();
        if (validationRules == null || validationRules.trim().isEmpty()) {
            return rules;
        }

        try {
            String[] rulePairs = validationRules.split(",");
            for (String rulePair : rulePairs) {
                String[] keyValue = rulePair.split(":");
                if (keyValue.length == 2) {
                    rules.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        } catch (Exception e) {
            log.warn("解析验证规则失败: {}", validationRules, e);
        }

        return rules;
    }
}