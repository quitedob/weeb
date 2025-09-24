package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.elasticsearch.MessageDocument; // 引入ES文档模型
import com.web.service.SearchService; // 引入搜索服务
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

    /**
     * 搜索消息内容（分页）
     * @param q 关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return data: { list, total }
     */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchMessages(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 检查Elasticsearch是否可用
        if (elasticsearchOperations == null) {
            log.warn("Elasticsearch is disabled, cannot perform message search");
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "搜索功能已禁用，请联系管理员启用Elasticsearch服务", data));
        }

        try {
            Criteria criteria = new Criteria("content").matches(q); // 构造条件匹配
            CriteriaQuery query = new CriteriaQuery(criteria, PageRequest.of(page, size)); // 分页条件查询

            SearchHits<MessageDocument> hits = elasticsearchOperations.search(query, MessageDocument.class); // 执行

            List<MessageDocument> list = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList()); // 提取内容

            Map<String, Object> data = new HashMap<>();
            data.put("list", list); // 结果列表
            data.put("total", hits.getTotalHits()); // 总条数

            return ResponseEntity.ok(ApiResponse.success(data)); // 返回
        } catch (Exception e) {
            log.error("搜索消息失败: {}", e.getMessage(), e);
            Map<String, Object> data = new HashMap<>();
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR, "搜索服务暂时不可用", data));
        }
    }

    /**
     * 搜索公开群组（分页）
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 搜索结果：{ list: 群组列表, total: 总数 }
     */
    @GetMapping("/group")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchGroups(@RequestParam("keyword") String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("搜索群组：keyword={}, page={}, size={}", keyword, page, size);

            Map<String, Object> data = searchService.searchGroups(keyword, page, size);

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
     * @return 搜索结果：{ list: 用户列表, total: 总数 }
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchUsers(@RequestParam("keyword") String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("搜索用户：keyword={}, page={}, size={}", keyword, page, size);

            Map<String, Object> data = searchService.searchUsers(keyword, page, size);

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
}
