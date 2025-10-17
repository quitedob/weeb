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

        // 设置默认排序方式
        String safeSortBy = sortBy != null ? sortBy : "time_desc";

        List<Group> groups = groupMapper.searchGroupsWithFilters(keyword.trim(), offset, size, startDate, endDate, safeSortBy);
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

        // 设置默认排序方式
        String safeSortBy = sortBy != null ? sortBy : "time_desc";

        List<User> users = userMapper.searchUsersWithFilters(keyword.trim(), offset, size, startDate, endDate, safeSortBy);
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

        // 设置默认排序方式
        String safeSortBy = sortBy != null ? sortBy : "time_desc";

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

        // 设置默认排序方式
        String safeSortBy = sortBy != null ? sortBy : "time_desc";

        List<Group> groups = groupMapper.searchGroupsAdvanced(keyword.trim(), offset, size, startDate, endDate,
                groupTypes, isPublic, safeSortBy, "desc");

        long total = groupMapper.countSearchGroupsAdvanced(keyword.trim(), startDate, endDate, groupTypes, isPublic);

        Map<String, Object> result = new HashMap<>();
        result.put("list", groups);
        result.put("total", total);

        return result;
    }
}