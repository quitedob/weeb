package com.web.service;

import com.web.model.Group;
import com.web.model.User;

import java.util.List;
import java.util.Map;

/**
 * 搜索服务接口
 * 定义搜索相关的业务逻辑契约
 */
public interface SearchService {

    /**
     * 搜索公开群组
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 搜索结果：{ list: 群组列表（包含群主用户名）, total: 总数 }
     */
    Map<String, Object> searchGroups(String keyword, int page, int size);

    /**
     * 搜索用户
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 搜索结果：{ list: 用户列表, total: 总数 }
     */
    Map<String, Object> searchUsers(String keyword, int page, int size);
}
