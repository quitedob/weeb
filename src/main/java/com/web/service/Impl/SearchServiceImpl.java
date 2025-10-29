package com.web.service.Impl;

import com.web.mapper.UserMapper;
import com.web.mapper.GroupMapper;
import com.web.model.User;
import com.web.model.Group;
import com.web.service.SearchService;
import com.web.exception.WeebException;
import com.web.util.SqlInjectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索服务实现类
 * 处理用户、群组等搜索功能
 */
@Service
@Transactional
public class SearchServiceImpl implements SearchService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GroupMapper groupMapper;

    /**
     * 构建用户搜索的排序SQL子句
     */
    private String buildUserSortClause(String sortBy, String keyword) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "relevance";
        }
        
        switch (sortBy.toLowerCase()) {
            case "relevance":
                return "CASE " +
                       "WHEN u.username = '" + keyword + "' THEN 1 " +
                       "WHEN u.nickname = '" + keyword + "' THEN 2 " +
                       "WHEN u.username LIKE '" + keyword + "%' THEN 3 " +
                       "WHEN u.nickname LIKE '" + keyword + "%' THEN 4 " +
                       "ELSE 5 END, u.id DESC";
            case "time_desc":
                return "u.registration_date DESC, u.id DESC";
            case "time_asc":
                return "u.registration_date ASC, u.id ASC";
            case "name_asc":
                return "u.username ASC, u.id ASC";
            case "name_desc":
                return "u.username DESC, u.id DESC";
            default:
                return "u.registration_date DESC, u.id DESC";
        }
    }

    /**
     * 构建群组搜索的排序SQL子句
     */
    private String buildGroupSortClause(String sortBy, String keyword) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "relevance";
        }
        
        switch (sortBy.toLowerCase()) {
            case "relevance":
                return "CASE " +
                       "WHEN g.group_name = '" + keyword + "' THEN 1 " +
                       "WHEN g.group_name LIKE '" + keyword + "%' THEN 2 " +
                       "ELSE 3 END, g.create_time DESC";
            case "time_desc":
                return "g.create_time DESC, g.id DESC";
            case "time_asc":
                return "g.create_time ASC, g.id ASC";
            case "name_asc":
                return "g.group_name ASC, g.id ASC";
            case "name_desc":
                return "g.group_name DESC, g.id DESC";
            default:
                return "g.create_time DESC, g.id DESC";
        }
    }

    @Override
    public Map<String, Object> searchGroups(String keyword, int page, int size) {
        try {
            // 输入验证
            if (keyword == null || keyword.trim().isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("list", List.of());
                result.put("total", 0L);
                return result;
            }

            // 使用SqlInjectionUtils进行SQL注入防护
            if (!SqlInjectionUtils.validateSqlParam(keyword, "搜索关键词")) {
                throw new WeebException("搜索关键词包含非法字符");
            }

            // 对搜索关键词进行安全清理
            keyword = SqlInjectionUtils.sanitizeInput(keyword.trim());

            // 分页参数验证
            if (page < 0) page = 0;
            if (size <= 0 || size > 100) size = 20;

            // 计算偏移量
            int offset = page * size;

            // 使用专门的搜索方法进行数据库级别的分页查询
            List<Group> groups = groupMapper.searchGroups(keyword, offset, size);
            long total = groupMapper.countSearchGroups(keyword);

            Map<String, Object> result = new HashMap<>();
            result.put("list", groups);
            result.put("total", total);

            return result;
        } catch (Exception e) {
            throw new WeebException("搜索群组失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> searchUsers(String keyword, int page, int size) {
        try {
            // 输入验证
            if (keyword == null || keyword.trim().isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("list", List.of());
                result.put("total", 0L);
                return result;
            }

            // 使用SqlInjectionUtils进行SQL注入防护
            if (!SqlInjectionUtils.validateSqlParam(keyword, "搜索关键词")) {
                throw new WeebException("搜索关键词包含非法字符");
            }

            // 对搜索关键词进行安全清理
            keyword = SqlInjectionUtils.sanitizeInput(keyword.trim());

            // 分页参数验证
            if (page < 0) page = 0;
            if (size <= 0 || size > 100) size = 20;

            // 计算偏移量
            int offset = page * size;

            // 使用专门的搜索方法进行数据库级别的分页查询
            List<User> users = userMapper.searchUsers(keyword, offset, size);
            long total = userMapper.countSearchUsers(keyword);

            // 过滤敏感信息（不返回密码等）
            users.forEach(user -> {
                user.setPassword(null); // 不返回密码
            });

            Map<String, Object> result = new HashMap<>();
            result.put("list", users);
            result.put("total", total);

            return result;
        } catch (Exception e) {
            throw new WeebException("搜索用户失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> searchGroupsWithFilters(String keyword, int page, int size, String startDate, String endDate, String sortBy) {
        // 参数验证
        if (keyword == null || keyword.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", List.of());
            result.put("total", 0L);
            return result;
        }

        // 计算偏移量
        int offset = page * size;

        // 构建排序SQL子句
        String sortClause = buildGroupSortClause(sortBy, keyword.trim());

        List<Group> groups = groupMapper.searchGroupsWithFilters(keyword.trim(), offset, size, startDate, endDate, sortClause);
        long total = groupMapper.countSearchGroupsWithFilters(keyword.trim(), startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("list", groups);
        result.put("total", total);

        return result;
    }

    @Override
    public Map<String, Object> searchUsersWithFilters(String keyword, int page, int size, String startDate, String endDate, String sortBy) {
        // 参数验证
        if (keyword == null || keyword.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", List.of());
            result.put("total", 0L);
            return result;
        }

        // 计算偏移量
        int offset = page * size;

        // 构建排序SQL子句
        String sortClause = buildUserSortClause(sortBy, keyword.trim());

        List<User> users = userMapper.searchUsersWithFilters(keyword.trim(), offset, size, startDate, endDate, sortClause);
        long total = userMapper.countSearchUsersWithFilters(keyword.trim(), startDate, endDate);

        // 过滤敏感信息（不返回密码等）
        users.forEach(user -> {
            user.setPassword(null); // 不返回密码
        });

        Map<String, Object> result = new HashMap<>();
        result.put("list", users);
        result.put("total", total);

        return result;
    }

    @Override
    public Map<String, Object> searchUsersAdvanced(String keyword, int page, int size, String startDate, String endDate,
                                                   List<Integer> userTypes, Integer onlineStatus,
                                                   Long minFansCount, Long maxFansCount, Long minTotalLikes, Long maxTotalLikes,
                                                   String sortBy) {
        // 参数验证
        if (keyword == null || keyword.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", List.of());
            result.put("total", 0L);
            return result;
        }

        // 计算偏移量
        int offset = page * size;

        // 使用SqlInjectionUtils验证并设置默认排序方式
        String safeSortBy = "time_desc";
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            // 验证排序参数是否安全
            if (SqlInjectionUtils.validateSortParams(sortBy, "desc")) {
                safeSortBy = sortBy.trim();
            }
        }

        List<User> users = userMapper.searchUsersAdvanced(keyword.trim(), offset, size, startDate, endDate,
                userTypes, onlineStatus, minFansCount, maxFansCount, minTotalLikes, maxTotalLikes, safeSortBy, "desc");

        long total = userMapper.countSearchUsersAdvanced(keyword.trim(), startDate, endDate,
                userTypes, onlineStatus, minFansCount, maxFansCount, minTotalLikes, maxTotalLikes);

        // 过滤敏感信息（不返回密码等）
        users.forEach(user -> {
            user.setPassword(null); // 不返回密码
        });

        Map<String, Object> result = new HashMap<>();
        result.put("list", users);
        result.put("total", total);

        return result;
    }

    @Override
    public Map<String, Object> searchGroupsAdvanced(String keyword, int page, int size, String startDate, String endDate,
                                                    List<Integer> groupTypes, Boolean isPublic, String sortBy) {
        // 参数验证
        if (keyword == null || keyword.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", List.of());
            result.put("total", 0L);
            return result;
        }

        // 计算偏移量
        int offset = page * size;

        // 使用SqlInjectionUtils验证并设置默认排序方式
        String safeSortBy = "time_desc";
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            // 验证排序参数是否安全
            if (SqlInjectionUtils.validateSortParams(sortBy, "desc")) {
                safeSortBy = sortBy.trim();
            }
        }

        List<Group> groups = groupMapper.searchGroupsAdvanced(keyword.trim(), offset, size, startDate, endDate,
                groupTypes, isPublic, safeSortBy, "desc");

        long total = groupMapper.countSearchGroupsAdvanced(keyword.trim(), startDate, endDate, groupTypes, isPublic);

        Map<String, Object> result = new HashMap<>();
        result.put("list", groups);
        result.put("total", total);

        return result;
    }
}