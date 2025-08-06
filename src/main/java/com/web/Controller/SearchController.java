package com.web.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局搜索控制器
 * 简化注释：搜索控制器
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    /**
     * Searches messages in Elasticsearch.
     * @param q The query string to search for in message content.
     * @param page The page number to retrieve (default is 0).
     * @param size The number of results per page (default is 10).
     * @return A Page of MessageDocument matching the search criteria.
     */
    @GetMapping("/messages")
    @ResponseBody
    public Map<String, Object> searchMessages(
            @RequestParam("q") String q, // q is required
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // TODO: 暂时返回空结果，等Elasticsearch配置完成后再实现
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "搜索功能开发中...");
        result.put("data", new HashMap<>());
        
        return result;
    }
}
