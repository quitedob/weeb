package com.web.service.impl;

import com.web.mapper.UserMapper;
import com.web.mapper.GroupMapper;
import com.web.model.User;
import com.web.model.Group;
import com.web.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索服务实现类
 * 处理用户、群组等搜索功能
 */
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Override
    public Map<String, Object> searchGroups(String keyword, int page, int size) {
        // 参数验证
        if (keyword == null || keyword.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", List.of());
            result.put("total", 0L);
            return result;
        }

        // 计算偏移量
        int offset = page * size;

        // 使用专门的搜索方法进行数据库级别的分页查询
        List<Group> groups = groupMapper.searchGroups(keyword.trim(), offset, size);
        long total = groupMapper.countSearchGroups(keyword.trim());

        Map<String, Object> result = new HashMap<>();
        result.put("list", groups);
        result.put("total", total);

        return result;
    }

    @Override
    public Map<String, Object> searchUsers(String keyword, int page, int size) {
        // 参数验证
        if (keyword == null || keyword.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", List.of());
            result.put("total", 0L);
            return result;
        }

        // 计算偏移量
        int offset = page * size;

        // 使用专门的搜索方法进行数据库级别的分页查询
        List<User> users = userMapper.searchUsers(keyword.trim(), offset, size);
        long total = userMapper.countSearchUsers(keyword.trim());

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

        // 构建排序条件
        String sortClause = "ORDER BY g.create_time DESC"; // 默认按创建时间排序
        if (sortBy != null) {
            switch (sortBy) {
                case "time_asc":
                    sortClause = "ORDER BY g.create_time ASC";
                    break;
                case "name_asc":
                    sortClause = "ORDER BY g.group_name ASC";
                    break;
                case "name_desc":
                    sortClause = "ORDER BY g.group_name DESC";
                    break;
                case "relevance":
                default:
                    // 相关度排序，使用默认的数据库排序逻辑
                    sortClause = "ORDER BY g.create_time DESC";
                    break;
            }
        }

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

        // 构建排序条件
        String sortClause = "ORDER BY u.create_time DESC"; // 默认按创建时间排序
        if (sortBy != null) {
            switch (sortBy) {
                case "time_asc":
                    sortClause = "ORDER BY u.create_time ASC";
                    break;
                case "name_asc":
                    sortClause = "ORDER BY u.username ASC";
                    break;
                case "name_desc":
                    sortClause = "ORDER BY u.username DESC";
                    break;
                case "relevance":
                default:
                    // 相关度排序，使用默认的数据库排序逻辑
                    sortClause = "ORDER BY u.create_time DESC";
                    break;
            }
        }

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
}