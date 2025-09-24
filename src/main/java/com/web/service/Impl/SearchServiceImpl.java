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
        // 群组搜索的简化实现
        // 实际需要在GroupMapper中添加搜索方法
        Map<String, Object> result = new HashMap<>();
        result.put("list", List.of()); // 空列表，需要具体实现
        result.put("total", 0);
        
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
}