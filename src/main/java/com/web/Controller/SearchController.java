package com.web.controller;

import com.web.mapper.elasticsearch.MessageSearchRepository;
import com.web.model.elasticsearch.MessageDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable; // Import for PageRequest
import org.springframework.data.elasticsearch.core.SearchHitSupport; // For SearchHits to Page conversion
import org.springframework.data.elasticsearch.core.SearchHits; // Return type of repository.search
import org.springframework.data.elasticsearch.core.query.Query; // Spring Data ES Query
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder; // For building native ES queries
import org.elasticsearch.index.query.QueryBuilders; // Elasticsearch native QueryBuilders

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局搜索控制器
 * 简化注释：搜索控制器
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private MessageSearchRepository messageSearchRepository;

    /**
     * Searches messages in Elasticsearch.
     * @param q The query string to search for in message content.
     * @param page The page number to retrieve (default is 0).
     * @param size The number of results per page (default is 10).
     * @return A Page of MessageDocument matching the search criteria.
     */
    @GetMapping("/messages")
    public Page<MessageDocument> searchMessages(
            @RequestParam("q") String q, // q is required
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Using matchQuery for full-text search on the 'content' field.
        // The 'content' field in MessageDocument is configured with IK Analyzer for Chinese.
        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("content", q))
                .withPageable(pageable)
                // .withSort(SortBuilders.scoreSort()) // Optional: sort by relevance score (default)
                // .withSort(SortBuilders.fieldSort("sendTime").order(SortOrder.DESC)) // Optional: sort by sendTime
                .build();

        // ElasticsearchRepository.search(Query) returns SearchHits<T>.
        // Convert SearchHits<T> to Page<T> using SearchHitSupport.
        SearchHits<MessageDocument> searchResult = messageSearchRepository.search(searchQuery);
        return SearchHitSupport.searchPageFor(searchResult, pageable);
    }
}
