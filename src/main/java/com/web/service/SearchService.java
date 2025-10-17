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
     * 搜索公开群组（带过滤器）
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param sortBy 排序方式 (可选)
     * @return 搜索结果：{ list: 群组列表（包含群主用户名）, total: 总数 }
     */
    Map<String, Object> searchGroupsWithFilters(String keyword, int page, int size, String startDate, String endDate, String sortBy);

    /**
     * 搜索用户
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 搜索结果：{ list: 用户列表, total: 总数 }
     */
    Map<String, Object> searchUsers(String keyword, int page, int size);

    /**
     * 搜索用户（带过滤器）
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param sortBy 排序方式 (可选)
     * @return 搜索结果：{ list: 用户列表, total: 总数 }
     */
    Map<String, Object> searchUsersWithFilters(String keyword, int page, int size, String startDate, String endDate, String sortBy);

    /**
     * 高级搜索用户（支持多维度过滤）
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param userTypes 用户类型列表 (可选)
     * @param onlineStatus 在线状态 (可选)
     * @param minFansCount 最小粉丝数 (可选)
     * @param maxFansCount 最大粉丝数 (可选)
     * @param minTotalLikes 最小总点赞数 (可选)
     * @param maxTotalLikes 最大总点赞数 (可选)
     * @param sortBy 排序方式 (可选)
     * @return 搜索结果：{ list: 用户列表, total: 总数 }
     */
    Map<String, Object> searchUsersAdvanced(String keyword, int page, int size, String startDate, String endDate,
                                           List<Integer> userTypes, Integer onlineStatus,
                                           Long minFansCount, Long maxFansCount, Long minTotalLikes, Long maxTotalLikes,
                                           String sortBy);

    /**
     * 高级搜索群组（支持多维度过滤）
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param groupTypes 群组类型列表 (可选)
     * @param isPublic 是否公开群组 (可选)
     * @param sortBy 排序方式 (可选)
     * @return 搜索结果：{ list: 群组列表, total: 总数 }
     */
    Map<String, Object> searchGroupsAdvanced(String keyword, int page, int size, String startDate, String endDate,
                                            List<Integer> groupTypes, Boolean isPublic, String sortBy);
}
