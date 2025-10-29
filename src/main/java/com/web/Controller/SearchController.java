package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.elasticsearch.MessageDocument; // 引入ES文档模型
import com.web.service.SearchService; // 引入搜索服务
import com.web.util.ValidationUtils; // 引入验证工具类
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired; // 注入
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest; // 分页请求
import org.springframework.data.elasticsearch.core.ElasticsearchOperations; // ES操作接口
import org.springframework.data.elasticsearch.core.SearchHit; // 命中
import org.springframework.data.elasticsearch.core.SearchHits; // 命中集合
import org.springframework.data.elasticsearch.core.query.Criteria; // 条件
import org.springframework.data.elasticsearch.core.query.CriteriaQuery; // 条件查询
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // 控制器注解

import java.util.HashMap; // Map实现
import java.util.List; // 列表
import java.util.Map; // Map
import java.util.stream.Collectors; // 流处理
import com.web.service.ArticleService; // 引入文章服务

/**
 * 全局搜索控制器
 * 简化注释：搜索控制器
 * 只有在启用Elasticsearch功能时才加载
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@ConditionalOnProperty(
    value = "elasticsearch.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SearchController {

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations; // 注入ES操作接口

    @Autowired
    private SearchService searchService; // 注入搜索服务

    @Autowired
    private ArticleService articleService; // 注入文章服务

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
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String messageTypes,
            @RequestParam(required = false) String userIds,
            @RequestParam(required = false) String groupIds,
            @RequestParam(defaultValue = "relevance") String sortBy) {

        // 验证搜索参数
        if (!ValidationUtils.validateSearchKeyword(q)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "搜索关键词无效", data));
        }

        if (!ValidationUtils.validatePageParams(page, size, "消息搜索")) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "分页参数无效", data));
        }

        if (!ValidationUtils.validateDateRange(startDate, endDate)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "日期范围无效", data));
        }

        String[] allowedSortValues = {"relevance", "time_desc", "time_asc", "username_asc", "username_desc"};
        if (!ValidationUtils.validateSortBy(sortBy, allowedSortValues)) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "排序参数无效", data));
        }

        if (!ValidationUtils.validateIdList(userIds, "用户ID列表") ||
            !ValidationUtils.validateIdList(groupIds, "群组ID列表")) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR, "ID列表参数无效", data));
        }

        // 检查Elasticsearch是否可用
        if (elasticsearchOperations == null) {
            log.warn("Elasticsearch is disabled, cannot perform message search");
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "搜索功能已禁用，请联系管理员启用Elasticsearch服务", data));
        }

        // 构建基础条件：内容匹配
        Criteria criteria = new Criteria("content").matches(q);

        // 添加日期范围过滤
        if (startDate != null && !startDate.isEmpty()) {
            criteria = criteria.and(new Criteria("timestamp").greaterThanEqual(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            criteria = criteria.and(new Criteria("timestamp").lessThanEqual(endDate));
        }

        // 添加消息类型过滤
        if (messageTypes != null && !messageTypes.isEmpty()) {
            String[] types = messageTypes.split(",");
            criteria = criteria.and(new Criteria("messageType").in((Object[]) types));
        }

        // 添加用户ID过滤
        if (userIds != null && !userIds.isEmpty()) {
            String[] users = userIds.split(",");
            criteria = criteria.and(new Criteria("fromUserId").in((Object[]) users));
        }

        // 添加群组ID过滤
        if (groupIds != null && !groupIds.isEmpty()) {
            String[] groups = groupIds.split(",");
            criteria = criteria.and(new Criteria("targetId").in((Object[]) groups));
        }

        // 构建分页查询
        PageRequest pageRequest = PageRequest.of(page, size);

        // 根据排序方式设置排序
        switch (sortBy) {
            case "time_desc":
                pageRequest = PageRequest.of(page, size,
                    org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "timestamp"));
                break;
            case "time_asc":
                pageRequest = PageRequest.of(page, size,
                    org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.ASC, "timestamp"));
                break;
            case "username_asc":
                pageRequest = PageRequest.of(page, size,
                    org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.ASC, "fromUsername"));
                break;
            case "username_desc":
                pageRequest = PageRequest.of(page, size,
                    org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "fromUsername"));
                break;
            default:
                // 相关度排序（默认）
                break;
        }

        CriteriaQuery query = new CriteriaQuery(criteria, pageRequest);

        SearchHits<MessageDocument> hits = elasticsearchOperations.search(query, MessageDocument.class); // 执行

        List<MessageDocument> list = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList()); // 提取内容

        Map<String, Object> data = new HashMap<>();
        data.put("list", list); // 结果列表
        data.put("total", hits.getTotalHits()); // 总条数

        return ResponseEntity.ok(ApiResponse.success(data)); // 返回
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
    @GetMapping("/group")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchGroups(@RequestParam("keyword") String keyword,
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

            Map<String, Object> data = searchService.searchGroupsWithFilters(keyword, page, size, startDate, endDate, sortBy);

            log.info("搜索群组完成：找到 {} 个群组", data.get("list") != null ? ((List<?>)data.get("list")).size() : 0);

            return ResponseEntity.ok(ApiResponse.success(data));

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
            @RequestParam("q") String q,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            switch (type.toLowerCase()) {
                case "group":
                    Map<String, Object> groupData = searchService.searchGroupsWithFilters(q, page, size, null, null, "relevance");
                    return ResponseEntity.ok(ApiResponse.success(groupData));
                    
                case "user":
                    Map<String, Object> userData = searchService.searchUsersWithFilters(q, page, size, null, null, "relevance");
                    return ResponseEntity.ok(ApiResponse.success(userData));
                    
                case "article":
                    Map<String, Object> articleData = articleService.searchArticlesWithFilters(q, page + 1, size, null, null, "created_at", "desc");
                    return ResponseEntity.ok(ApiResponse.success(articleData));
                    
                case "message":
                    if (elasticsearchOperations != null) {
                        Criteria criteria = new Criteria("content").matches(q);
                        CriteriaQuery query = new CriteriaQuery(criteria, PageRequest.of(page, size));
                        SearchHits<MessageDocument> hits = elasticsearchOperations.search(query, MessageDocument.class);
                        
                        Map<String, Object> messageData = new HashMap<>();
                        messageData.put("list", hits.getSearchHits().stream()
                                .map(SearchHit::getContent)
                                .collect(Collectors.toList()));
                        messageData.put("total", hits.getTotalHits());
                        return ResponseEntity.ok(ApiResponse.success(messageData));
                    } else {
                        Map<String, Object> emptyData = new HashMap<>();
                        emptyData.put("list", java.util.Collections.emptyList());
                        emptyData.put("total", 0L);
                        return ResponseEntity.ok(ApiResponse.success(emptyData));
                    }
                    
                case "all":
                default:
                    return searchAll(q, page, size);
            }
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
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        try {
            log.info("综合搜索：q={}, page={}, size={}", q, page, size);

            Map<String, Object> result = new HashMap<>();

            // 并行搜索各种类型
            try {
                result.put("users", searchService.searchUsers(q, page, size));
            } catch (Exception e) {
                log.warn("搜索用户失败：{}", e.getMessage());
                result.put("users", Map.of("list", List.of(), "total", 0L));
            }

            try {
                result.put("groups", searchService.searchGroups(q, page, size));
            } catch (Exception e) {
                log.warn("搜索群组失败：{}", e.getMessage());
                result.put("groups", Map.of("list", List.of(), "total", 0L));
            }

            try {
                result.put("articles", articleService.searchArticles(q, page + 1, size, "created_at", "desc"));
            } catch (Exception e) {
                log.warn("搜索文章失败：{}", e.getMessage());
                result.put("articles", Map.of("list", List.of(), "total", 0L));
            }

            // 只有在ES可用时才搜索消息
            if (elasticsearchOperations != null) {
                try {
                    Criteria criteria = new Criteria("content").matches(q);
                    CriteriaQuery query = new CriteriaQuery(criteria, PageRequest.of(page, size));
                    SearchHits<MessageDocument> hits = elasticsearchOperations.search(query, MessageDocument.class);

                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("list", hits.getSearchHits().stream()
                            .map(SearchHit::getContent)
                            .collect(Collectors.toList()));
                    messageData.put("total", hits.getTotalHits());
                    result.put("messages", messageData);
                } catch (Exception e) {
                    log.warn("搜索消息失败：{}", e.getMessage());
                    result.put("messages", Map.of("list", List.of(), "total", 0L));
                }
            } else {
                result.put("messages", Map.of("list", List.of(), "total", 0L, "disabled", true));
            }

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("综合搜索失败：{}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "综合搜索失败", Map.of()));
        }
    }
}
