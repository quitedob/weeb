package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.Article;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.service.ArticleService;
import com.web.vo.article.ArticleCreateVo;
import com.web.vo.article.ArticleUpdateVo;
import com.web.vo.article.ArticleSearchAdvancedVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;  // 注意导入
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/articles")
public class ArticleCenterController {

    private final ArticleService articleService;
    private static final Logger logger = LoggerFactory.getLogger(ArticleCenterController.class);

    // =========================
    // 新增：注入 AuthService
    // =========================
    @Autowired
    private AuthService authService;

    // 用构造器注入 ArticleService
    @Autowired
    public ArticleCenterController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /**
     * 根据ID获取文章信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> getArticle(@PathVariable Long id) {
        try {
            Article article = articleService.getArticleById(id);
            if (article == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.articleNotFound(ApiResponse.Messages.ARTICLE_NOT_FOUND));
            }
            return ResponseEntity.ok(ApiResponse.success(article));
        } catch (Exception e) {
            logger.error("错误 获取 文章 从 id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    
    /**
     * 原本的方法：根据 userId 获取用户各类统计信息
     * GET /articles/userinform?userId=123
     */
    @GetMapping("/userinform")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserInformation(@RequestParam Long userId) {
        try {
            Map<String, Object> userInfo = articleService.getUserInformation(userId);
            return ResponseEntity.ok(ApiResponse.success(userInfo));
        } catch (Exception e) {
            logger.error("获取信息时出错,用户ID为 {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }


    /**
     * 文章点赞
     * 请求示例：POST /articles/123/like
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<String>> likeArticle(@PathVariable Long id) {
        try {
            boolean success = articleService.likeArticle(id);
            if (!success) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.likeOperationFailed(ApiResponse.Messages.LIKE_FAILED));
            }
            return ResponseEntity.ok(ApiResponse.success(ApiResponse.Messages.LIKE_SUCCESS));
        } catch (Exception e) {
            logger.error("Error liking article with id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 增加粉丝（订阅用户）
     * 请求示例：POST /articles/subscribe?targetUserId=456
     */
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<String>> subscribe(
            @RequestParam Long targetUserId,
            @Userid Long authenticatedUserId) {
        try {
            // 安全验证：使用认证用户ID，防止恶意订阅
            boolean success = articleService.subscribeUser(authenticatedUserId, targetUserId);
            if (!success) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.subscribeOperationFailed(ApiResponse.Messages.SUBSCRIBE_FAILED));
            }
            return ResponseEntity.ok(ApiResponse.success(ApiResponse.Messages.SUBSCRIBE_SUCCESS));
        } catch (Exception e) {
            logger.error("订阅失败在用户 {} 和 {}之间", authenticatedUserId, targetUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }


    /**
     * 修改文章内容和标题，并更新最后更新时间
     * 请求示例：PUT /articles/123
     * 请求体：ArticleUpdateVo
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'ARTICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<String>> editArticle(
            @PathVariable Long id,
            @RequestBody @Valid ArticleUpdateVo updateVo,
            @Userid Long authenticatedUserId) {
        try {
            // 将VO转换为Article实体
            Article article = new Article();
            article.setArticleTitle(updateVo.getArticleTitle());
            article.setArticleContent(updateVo.getArticleContent());
            article.setArticleLink(updateVo.getArticleLink());
            article.setCategoryId(updateVo.getCategoryId());
            article.setStatus(updateVo.getStatus());
            
            // 安全验证：确保用户只能编辑自己的文章
            boolean updated = articleService.editArticle(id, article, authenticatedUserId);
            if (!updated) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.articleUpdateFailed(ApiResponse.Messages.ARTICLE_UPDATE_FAILED));
            }
            return ResponseEntity.ok(ApiResponse.success(ApiResponse.Messages.ARTICLE_UPDATE_SUCCESS));
        } catch (Exception e) {
            logger.error("文章编辑失败，ID为 {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 增加文章金币
     * 请求示例：POST /articles/123/addcoin?amount=10.0
     */
    @PostMapping("/{id}/addcoin")
    public ResponseEntity<ApiResponse<String>> addCoin(
            @PathVariable Long id,
            @RequestParam Double amount) {
        try {
            boolean updated = articleService.addCoin(id, amount);
            if (!updated) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.coinOperationFailed(ApiResponse.Messages.COIN_ADD_FAILED));
            }
            return ResponseEntity.ok(ApiResponse.success(ApiResponse.Messages.COIN_ADD_SUCCESS));
        } catch (Exception e) {
            logger.error("添加金币失败在文章ID为 {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 增加阅读数量
     * 请求示例：POST /articles/123/read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> increaseReadCount(@PathVariable Long id) {
        try {
            boolean updated = articleService.increaseReadCount(id);
            if (!updated) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.readCountUpdateFailed(ApiResponse.Messages.READ_COUNT_UPDATE_FAILED));
            }
            return ResponseEntity.ok(ApiResponse.success(ApiResponse.Messages.READ_COUNT_UPDATE_SUCCESS));
        } catch (Exception e) {
            logger.error("Error increasing read count for article with id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 创建新文章
     * 请求示例：POST /articles/new
     * 请求体：ArticleCreateVo 对象 JSON 格式
     */
    @PostMapping("/new")
    @PreAuthorize("hasPermission(null, 'ARTICLE_CREATE_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createArticle(
            @RequestBody @Valid ArticleCreateVo createVo, 
            @Userid Long authenticatedUserId) {
        try {
            // 将VO转换为Article实体
            Article article = new Article();
            article.setUserId(authenticatedUserId); // 安全验证：强制使用认证用户ID
            article.setArticleTitle(createVo.getArticleTitle());
            article.setArticleContent(createVo.getArticleContent());
            article.setArticleLink(createVo.getArticleLink());
            article.setCategoryId(createVo.getCategoryId());
            article.setStatus(createVo.getStatus());
            
            int result = articleService.createArticle(article);
            if (result <= 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.articleCreateFailed(ApiResponse.Messages.ARTICLE_CREATE_FAILED));
            }
            Map<String, Object> data = Map.of(
                "id", article.getId(),
                "message", "文章创建成功，ID为: " + article.getId()
            );
            return ResponseEntity.ok(ApiResponse.success(ApiResponse.Messages.ARTICLE_CREATE_SUCCESS, data));
        } catch (Exception e) {
            logger.error("创建文章失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }
    /**
     * 新增一个接口：获取某个用户所有文章的订阅数、点赞数、金币、阅读量和收藏数
     * 请求示例：POST /articles/userinform?userId=123
     */
    @PostMapping("/userinform")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserAllArticlesStats(@RequestParam Long userId) {
        try {
            Map<String, Object> stats = articleService.getUserAllArticlesStats(userId);
            if (stats == null || stats.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("用户文章统计数据未找到"));
            }
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            logger.error("获取用户 {} 的文章统计数据时出错", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 获取当前用户的所有文章标题、内容、阅读数量、点赞数量
     * 请求示例：GET /articles/myarticles?userId=123
     */
    @GetMapping("/myarticles")
    public ResponseEntity<ApiResponse<List<Article>>> getUserArticles(@RequestParam Long userId) {
        try {
            List<Article> articles = articleService.getArticlesByUserId(userId);
            if (articles == null || articles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("用户文章列表未找到"));
            }
            return ResponseEntity.ok(ApiResponse.success(articles));
        } catch (Exception e) {
            logger.error("获取用户 {} 的文章列表时出错", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 收藏文章
     * POST /articles/{id}/favorite
     */
    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<String>> favoriteArticle(@PathVariable Long id, @Userid Long authenticatedUserId) {
        try {
            boolean success = articleService.favoriteArticle(id, authenticatedUserId);
            if (!success) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("收藏失败"));
            }
            return ResponseEntity.ok(ApiResponse.success("收藏成功"));
        } catch (Exception e) {
            logger.error("收藏文章失败，文章ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 取消收藏文章
     * DELETE /articles/{id}/favorite
     */
    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<String>> unfavoriteArticle(@PathVariable Long id, @Userid Long authenticatedUserId) {
        try {
            boolean success = articleService.unfavoriteArticle(id, authenticatedUserId);
            if (!success) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("取消收藏失败"));
            }
            return ResponseEntity.ok(ApiResponse.success("取消收藏成功"));
        } catch (Exception e) {
            logger.error("取消收藏文章失败，文章ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 检查用户是否已收藏文章
     * GET /articles/{id}/favorite/status
     */
    @GetMapping("/{id}/favorite/status")
    public ResponseEntity<ApiResponse<Boolean>> checkFavoriteStatus(@PathVariable Long id, @Userid Long authenticatedUserId) {
        try {
            boolean isFavorited = articleService.isArticleFavoritedByUser(id, authenticatedUserId);
            return ResponseEntity.ok(ApiResponse.success(isFavorited));
        } catch (Exception e) {
            logger.error("检查收藏状态失败，文章ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 获取用户收藏的文章列表
     * GET /articles/favorites?page=1&pageSize=10
     */
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserFavoriteArticles(
            @Userid Long authenticatedUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Map<String, Object> favoriteArticles = articleService.getUserFavoriteArticles(authenticatedUserId, page, pageSize);
            return ResponseEntity.ok(ApiResponse.success(favoriteArticles));
        } catch (Exception e) {
            logger.error("获取用户收藏文章失败，用户ID: {}", authenticatedUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 获取文章分类列表
     * GET /api/articles/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<com.web.model.ArticleCategory>>> getCategories() {
        try {
            List<com.web.model.ArticleCategory> categories = articleService.getAllCategories();
            return ResponseEntity.ok(ApiResponse.success(categories));
        } catch (Exception e) {
            logger.error("获取分类列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 获取推荐文章列表
     * GET /articles/recommended?page=1&pageSize=10
     */
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendedArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Map<String, Object> recommendedArticles = articleService.getRecommendedArticles(page, pageSize);
            return ResponseEntity.ok(ApiResponse.success(recommendedArticles));
        } catch (Exception e) {
            logger.error("获取推荐文章失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 搜索文章
     * GET /articles/search?query=关键词&page=1&pageSize=10&sortBy=created_at&sortOrder=desc
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchArticles(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            Map<String, Object> searchResult = articleService.searchArticles(query, page, pageSize, sortBy, sortOrder);
            return ResponseEntity.ok(ApiResponse.success(searchResult));
        } catch (Exception e) {
            logger.error("搜索文章失败，关键词: {}", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 高级搜索文章
     * GET /articles/search/advanced?query=...&page=1&pageSize=10...
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchArticlesAdvanced(
            @Valid ArticleSearchAdvancedVo searchVo) {
        try {
            Map<String, Object> searchResult = articleService.searchArticlesAdvanced(searchVo);
            return ResponseEntity.ok(ApiResponse.success(searchResult));
        } catch (Exception e) {
            logger.error("高级搜索文章失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * [修改] 获取articles表中所有文章的基本信息（支持分页和排序）
     * GET /articles/getall?page=1&pageSize=10&sortBy=created_at&sortOrder=desc
     */
    @GetMapping("/getall") // Changed from @PostMapping to @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            Map<String, Object> articlesMap = articleService.getAllArticles(page, pageSize, sortBy, sortOrder); // Calls new service method
            // The service now returns a map like {"list": [...], "total": ...}
            // This map can be directly returned.
            return ResponseEntity.ok(ApiResponse.success(articlesMap));
        } catch (Exception e) {
            logger.error("获取所有文章时出错", e); // Error message in Chinese as per existing log
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }
    // RESTful 删除文章接口
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'ARTICLE_DELETE_OWN')")
    public ResponseEntity<ApiResponse<String>> deleteArticle(@PathVariable("id") Long id, @Userid Long authenticatedUserId) {
        try {
            // 安全验证：确保用户只能删除自己的文章
            boolean deleted = articleService.deleteArticle(id, authenticatedUserId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(ApiResponse.Messages.ARTICLE_DELETE_SUCCESS));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.articleDeleteFailed("未找到该文章或无权删除，删除失败"));
            }
        } catch (Exception e) {
            logger.error("删除文章失败，文章ID为 {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

}
