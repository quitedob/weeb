package com.web.controller;

import com.web.common.ApiResponse;
import com.web.annotation.Userid;
import com.web.service.MessageSearchService;
import org.springframework.security.access.AccessDeniedException;
import com.web.service.SearchService; // 引入搜索服务
import com.web.util.ValidationUtils; // 引入验证工具类
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired; // 注入
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // 控制器注解

import java.util.HashMap; // Map实现
import java.util.List; // 列表
import java.util.Map; // Map
import com.web.service.ArticleService; // 引入文章服务

/**
 * 全局搜索控制器
 * 简化注释：搜索控制器
 * 消息结果按当前聊天成员权限查询数据库。
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private MessageSearchService messageSearchService;

    @Autowired
    private SearchService searchService; // 注入搜索服务

    @Autowired
    private ArticleService articleService; // 注入文章服务

    @Autowired
    private com.web.service.GroupService groupService;

    /**
     * 搜索消息内容（分页）
     * @param q 关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param messageTypes 消息类型列表 (可选)
     * @param userIds 用户ID列表 (可选)
     * @param groupIds 群组ID列表 (可选)
     * @param sortBy 排序方式 (可选)
     * @return data: { list, total }
     */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchMessages(
            @Userid Long userId,
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String messageTypes,
            @RequestParam(required = false) String userIds,
            @RequestParam(required = false) String groupIds,
            @RequestParam(defaultValue = "relevance") String sortBy) {
        return ResponseEntity.ok(ApiResponse.success(messageSearchService.search(userId, q, page, size,
                startDate, endDate, messageTypes, userIds, groupIds, sortBy)));
    }

    /**
     * 搜索公开群组（分页）
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param sortBy 排序方式 (可选)
     * @return 搜索结果：{ list: 群组列表, total: 总数 }
     */
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchGroups(String keyword, int page, int size,
            String startDate, String endDate, String sortBy) {
        return searchGroupsForUser(null, keyword, page, size, startDate, endDate, sortBy);
    }

    @GetMapping("/group")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchGroupsForUser(@Userid Long userId, @RequestParam("keyword") String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            @RequestParam(defaultValue = "relevance") String sortBy) {

        // 验证搜索参数
        if (!ValidationUtils.validateSearchKeyword(keyword)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "搜索关键词无效", data));
        }

        if (!ValidationUtils.validatePageParams(page, size, "群组搜索")) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "分页参数无效", data));
        }

        if (!ValidationUtils.validateDateRange(startDate, endDate)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "日期范围无效", data));
        }

        String[] allowedSortValues = {"relevance", "time_desc", "time_asc", "name_asc", "name_desc"};
        if (!ValidationUtils.validateSortBy(sortBy, allowedSortValues)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "排序参数无效", data));
        }

        try {
            log.info("搜索群组：keyword={}, page={}, size={}, startDate={}, endDate={}, sortBy={}",
                keyword, page, size, startDate, endDate, sortBy);

            Map<String, Object> data = withGroupRoles(userId,
                    searchService.searchGroupsWithFilters(keyword, page, size, startDate, endDate, sortBy));

            log.info("搜索群组完成：找到 {} 个群组", data.get("list") != null ? ((List<?>)data.get("list")).size() : 0);

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("搜索群组失败：{}", e.getMessage(), e);
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "搜索群组失败", data));
        }
    }

    /**
     * 搜索用户（分页）
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param sortBy 排序方式 (可选)
     * @return 搜索结果：{ list: 用户列表, total: 总数 }
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchUsers(@RequestParam("keyword") String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            @RequestParam(defaultValue = "relevance") String sortBy) {

        // 验证搜索参数
        if (!ValidationUtils.validateSearchKeyword(keyword)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "搜索关键词无效", data));
        }

        if (!ValidationUtils.validatePageParams(page, size, "用户搜索")) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "分页参数无效", data));
        }

        if (!ValidationUtils.validateDateRange(startDate, endDate)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "日期范围无效", data));
        }

        String[] allowedSortValues = {"relevance", "time_desc", "time_asc", "name_asc", "name_desc"};
        if (!ValidationUtils.validateSortBy(sortBy, allowedSortValues)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "排序参数无效", data));
        }

        try {
            log.info("搜索用户：keyword={}, page={}, size={}, startDate={}, endDate={}, sortBy={}",
                keyword, page, size, startDate, endDate, sortBy);

            Map<String, Object> data = searchService.searchUsersWithFilters(keyword, page, size, startDate, endDate, sortBy);

            log.info("搜索用户完成：找到 {} 个用户", data.get("list") != null ? ((List<?>)data.get("list")).size() : 0);

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("搜索用户失败：{}", e.getMessage(), e);
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "搜索用户失败", data));
        }
    }

    /**
     * 搜索文章（分页）
     * @param query 搜索关键词
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param sortBy 排序字段 (created_at, updated_at, title, relevance)
     * @param sortOrder 排序方向 (asc, desc)
     * @return 搜索结果：{ list: 文章列表, total: 总数 }
     */
    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchArticles(
            @RequestParam("query") String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        // 验证搜索参数
        if (!ValidationUtils.validateSearchKeyword(query)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "搜索关键词无效", data));
        }

        if (!ValidationUtils.validatePageParams(page, pageSize, "文章搜索")) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "分页参数无效", data));
        }

        if (!ValidationUtils.validateDateRange(startDate, endDate)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "日期范围无效", data));
        }

        String[] allowedSortValues = {"created_at", "updated_at", "title", "relevance"};
        if (!ValidationUtils.validateSortBy(sortBy, allowedSortValues)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "排序参数无效", data));
        }

        String[] allowedSortOrders = {"asc", "desc"};
        if (!ValidationUtils.validateSortBy(sortOrder, allowedSortOrders)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "排序方向无效", data));
        }

        try {
            log.info("搜索文章：query={}, page={}, pageSize={}, startDate={}, endDate={}, sortBy={}, sortOrder={}",
                query, page, pageSize, startDate, endDate, sortBy, sortOrder);

            Map<String, Object> data = articleService.searchArticlesWithFilters(query, page, pageSize, startDate, endDate, sortBy, sortOrder);

            log.info("搜索文章完成：找到 {} 篇文章", data.get("list") != null ? ((List<?>)data.get("list")).size() : 0);

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("搜索文章失败：{}", e.getMessage(), e);
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0L);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "搜索文章失败", data));
        }
    }

    /**
     * 统一搜索接口 - 根据type参数搜索不同类型
     * @param q 搜索关键词
     * @param type 搜索类型 (group, user, article, message, all)
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 搜索结果
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> unifiedSearch(
            @Userid Long userId,
            @RequestParam("q") String q,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        MessageSearchService.validateRequest(userId, q, page, size);
        try {
            switch (type.toLowerCase(java.util.Locale.ROOT)) {
                case "group":
                    Map<String, Object> groupData = withGroupRoles(userId,
                            searchService.searchGroupsWithFilters(q, page, size, null, null, "relevance"));
                    return ResponseEntity.ok(ApiResponse.success(groupData));
                    
                case "user":
                    Map<String, Object> userData = searchService.searchUsersWithFilters(q, page, size, null, null, "relevance");
                    return ResponseEntity.ok(ApiResponse.success(userData));
                    
                case "article":
                    Map<String, Object> articleData = articleService.searchArticlesWithFilters(q, page + 1, size, null, null, "created_at", "desc");
                    return ResponseEntity.ok(ApiResponse.success(articleData));
                    
                case "message":
                    return searchMessages(userId, q, page, size, null, null, null, null, null, "relevance");

                case "all":
                default:
                    return searchAll(userId, q, page, size);
            }
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("统一搜索失败：type={}, q={}", type, q, e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("list", java.util.Collections.emptyList());
            errorData.put("total", 0L);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "搜索失败", errorData));
        }
    }

    /**
     * 综合搜索 - 一次搜索所有类型
     * @param q 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 综合搜索结果：{ users: {}, groups: {}, messages: {}, articles: {} }
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchAll(
            @Userid Long userId,
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        MessageSearchService.validateMessageRequest(userId, q, page, size);
        try {
            log.info("综合搜索：q={}, page={}, size={}", q, page, size);

            Map<String, Object> result = new HashMap<>();

            // 各类型保留独立的结果与分页。
            try {
                result.put("users", searchService.searchUsers(q, page, size));
            } catch (AccessDeniedException | IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.warn("搜索用户失败：{}", e.getMessage());
                result.put("users", Map.of("list", List.of(), "total", 0L));
            }

            try {
                result.put("groups", withGroupRoles(userId, searchService.searchGroups(q, page, size)));
            } catch (AccessDeniedException | IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.warn("搜索群组失败：{}", e.getMessage());
                result.put("groups", Map.of("list", List.of(), "total", 0L));
            }

            try {
                result.put("articles", articleService.searchArticles(q, page + 1, size, "created_at", "desc"));
            } catch (AccessDeniedException | IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.warn("搜索文章失败：{}", e.getMessage());
                result.put("articles", Map.of("list", List.of(), "total", 0L));
            }

            // 消息检索始终使用当前数据库中的成员权限。
            result.put("messages", messageSearchService.search(userId, q, page, size,
                    null, null, null, null, null, "relevance"));

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("综合搜索失败：{}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "综合搜索失败", Map.of()));
        }
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> withGroupRoles(Long userId, Map<String, Object> page) {
        Map<String, Object> result = new HashMap<>(page);
        List<com.web.model.Group> groups = (List<com.web.model.Group>) page.getOrDefault("list", List.of());
        result.put("list", groupService.withCurrentUserRoles(userId, groups));
        return result;
    }
}
