package com.web.Controller;

import com.web.model.elasticsearch.MessageDocument; // 引入ES文档模型
import org.springframework.beans.factory.annotation.Autowired; // 注入
import org.springframework.data.domain.PageRequest; // 分页请求
import org.springframework.data.elasticsearch.core.ElasticsearchOperations; // ES操作接口
import org.springframework.data.elasticsearch.core.SearchHit; // 命中
import org.springframework.data.elasticsearch.core.SearchHits; // 命中集合
import org.springframework.data.elasticsearch.core.query.Criteria; // 条件
import org.springframework.data.elasticsearch.core.query.CriteriaQuery; // 条件查询
import org.springframework.web.bind.annotation.*; // 控制器注解

import java.util.HashMap; // Map实现
import java.util.List; // 列表
import java.util.Map; // Map
import java.util.stream.Collectors; // 流处理

/**
 * 全局搜索控制器
 * 简化注释：搜索控制器
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations; // 注入ES操作接口

    /**
     * 搜索消息内容（分页）
     * @param q 关键词
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return data: { list, total }
     */
    @GetMapping("/messages")
    @ResponseBody
    public Map<String, Object> searchMessages(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Criteria criteria = new Criteria("content").matches(q); // 构造条件匹配
        CriteriaQuery query = new CriteriaQuery(criteria, PageRequest.of(page, size)); // 分页条件查询

        SearchHits<MessageDocument> hits = elasticsearchOperations.search(query, MessageDocument.class); // 执行

        List<MessageDocument> list = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList()); // 提取内容

        Map<String, Object> data = new HashMap<>();
        data.put("list", list); // 结果列表
        data.put("total", hits.getTotalHits()); // 总条数

        Map<String, Object> result = new HashMap<>();
        result.put("success", true); // 成功标志
        result.put("data", data); // 数据
        return result; // 返回
    }
}
