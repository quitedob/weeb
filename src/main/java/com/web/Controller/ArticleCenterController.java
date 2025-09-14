package com.web.Controller;

import com.web.annotation.Userid;
import com.web.model.Article;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.service.ArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;  // 注意导入
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/articles")
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
    public ResponseEntity<Article> getArticle(@PathVariable Long id) {
        try {
            Article article = articleService.getArticleById(id);
            if (article == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(article);
        } catch (Exception e) {
            logger.error("错误 获取 文章 从 id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 通过用户名获取用户信息，再调用原来的 getUserInformation(userId) 方法
     * GET /articles/userinform-by-username?username=alice
     */
    /**
     * 通过用户名获取用户信息，包括注册天数
     * GET /articles/userinform-by-username?username=alice
     */
    @GetMapping("/userinform-by-username")
    public ResponseEntity<Map<String, Object>> getUserInformationByUsername(
            @RequestParam String username) {
        try {
            // 1. 通过用户名获取 User 对象
            User user = authService.findDateByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            // 2. 提取 userId 和 registrationDate
            Long userId = user.getId();
            Date registrationDate = user.getRegistrationDate();

            // 3. 计算从 registrationDate 到现在经过的天数
            long daysPassed = 0;
            if (registrationDate != null) {
                long diffInMillis = new Date().getTime() - registrationDate.getTime();
                daysPassed = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
            }

            // 4. 调用原本的方法获取用户详细信息
            ResponseEntity<Map<String, Object>> response = getUserInformation(userId);
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> userInfo = response.getBody();
                if (userInfo != null) {
                    // 将计算出的注册天数加入返回数据
                    userInfo.put("registrationDays", daysPassed);
                    userInfo.put("registrationDate", registrationDate);
                }
                return ResponseEntity.ok(userInfo);
            }
            return response;
        } catch (Exception e) {
            logger.error("通过用户名获取信息时出错, 用户名为 {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 原本的方法：根据 userId 获取用户各类统计信息
     * GET /articles/userinform?userId=123
     */
    @GetMapping("/userinform")
    public ResponseEntity<Map<String, Object>> getUserInformation(@RequestParam Long userId) {
        try {
            Map<String, Object> userInfo = articleService.getUserInformation(userId);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            logger.error("获取信息时出错,用户ID为 {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * 文章点赞
     * 请求示例：POST /articles/123/like
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<String> likeArticle(@PathVariable Long id) {
        try {
            boolean success = articleService.likeArticle(id);
            if (!success) {
                return ResponseEntity.badRequest().body("点赞失败");
            }
            return ResponseEntity.ok("点赞成功");
        } catch (Exception e) {
            logger.error("Error liking article with id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("互联网服务失败");
        }
    }

    /**
     * 增加粉丝（订阅用户）
     * 请求示例：POST /articles/subscribe?targetUserId=456
     */
    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(
            @RequestParam Long targetUserId,
            @Userid Long authenticatedUserId) {
        try {
            // 安全验证：使用认证用户ID，防止恶意订阅
            boolean success = articleService.subscribeUser(authenticatedUserId, targetUserId);
            if (!success) {
                return ResponseEntity.badRequest().body("订阅失败");
            }
            return ResponseEntity.ok("订阅成功");
        } catch (Exception e) {
            logger.error("订阅失败在用户 {} 和 {}之间", authenticatedUserId, targetUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("互联网服务失败");
        }
    }


    /**
     * 修改文章内容和标题，并更新最后更新时间
     * 请求示例：PUT /articles/123
     * 请求体：{ "articleTitle": "...", "articleLink": "...", ... }
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> editArticle(
            @PathVariable Long id,
            @RequestBody Article article,
            @Userid Long authenticatedUserId) {
        try {
            // 安全验证：确保用户只能编辑自己的文章
            boolean updated = articleService.editArticle(id, article, authenticatedUserId);
            if (!updated) {
                return ResponseEntity.badRequest().body("更新失败");
            }
            return ResponseEntity.ok("文章更新成功");
        } catch (Exception e) {
            logger.error("文章编辑失败，ID为 {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("互联网服务失败");
        }
    }

    /**
     * 增加文章金币
     * 请求示例：POST /articles/123/addcoin?amount=10.0
     */
    @PostMapping("/{id}/addcoin")
    public ResponseEntity<String> addCoin(
            @PathVariable Long id,
            @RequestParam Double amount) {
        try {
            boolean updated = articleService.addCoin(id, amount);
            if (!updated) {
                return ResponseEntity.badRequest().body("添加金币失败");
            }
            return ResponseEntity.ok("添加金币成功");
        } catch (Exception e) {
            logger.error("添加金币失败在文章ID为 {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("互联网失败");
        }
    }

    /**
     * 增加阅读数量
     * 请求示例：POST /articles/123/read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<String> increaseReadCount(@PathVariable Long id) {
        try {
            boolean updated = articleService.increaseReadCount(id);
            if (!updated) {
                return ResponseEntity.badRequest().body("阅读量增加失败");
            }
            return ResponseEntity.ok("阅读量增加成功");
        } catch (Exception e) {
            logger.error("Error increasing read count for article with id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("互联网失败");
        }
    }

    /**
     * 创建新文章
     * 请求示例：POST /articles/new
     * 请求体：Article 对象 JSON 格式
     */
    @PostMapping("/new")
    public ResponseEntity<String> createArticle(@RequestBody Article article, @Userid Long authenticatedUserId) {
        try {
            // 安全验证：强制使用认证用户ID，忽略请求体中的userId
            article.setUserId(authenticatedUserId);
            int result = articleService.createArticle(article);
            if (result <= 0) {
                return ResponseEntity.badRequest().body("文章创建失败");
            }
            return ResponseEntity.ok("文章创建成功，ID为: " + article.getId());
        } catch (Exception e) {
            logger.error("创建文章失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("服务器错误");
        }
    }
    /**
     * 新增一个接口：获取某个用户所有文章的订阅数、点赞数、金币、阅读量和收藏数
     * 请求示例：POST /articles/userinform?userId=123
     */
    @PostMapping("/userinform")
    public ResponseEntity<Map<String, Object>> getUserAllArticlesStats(@RequestParam Long userId) {
        try {
            Map<String, Object> stats = articleService.getUserAllArticlesStats(userId);
            if (stats == null || stats.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取用户 {} 的文章统计数据时出错", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取当前用户的所有文章标题、内容、阅读数量、点赞数量
     * 请求示例：POST /articles/myarticles?userId=123
     */
    @GetMapping("/myarticles")
    public ResponseEntity<List<Article>> getUserArticles(@RequestParam Long userId) {
        try {
            List<Article> articles = articleService.getArticlesByUserId(userId);
            if (articles == null || articles.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("获取用户 {} 的文章列表时出错", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * [修改] 获取articles表中所有文章的基本信息（支持分页）
     * GET /articles/getall?page=1&pageSize=10
     */
    @GetMapping("/getall") // Changed from @PostMapping to @GetMapping
    public ResponseEntity<Map<String, Object>> getAllArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Map<String, Object> articlesMap = articleService.getAllArticles(page, pageSize); // Calls new service method
            // The service now returns a map like {"list": [...], "total": ...}
            // This map can be directly returned.
            return ResponseEntity.ok(articlesMap);
        } catch (Exception e) {
            logger.error("获取所有文章时出错", e); // Error message in Chinese as per existing log
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // RESTful 删除文章接口
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteArticle(@PathVariable("id") Long id, @Userid Long authenticatedUserId) {
        try {
            // 安全验证：确保用户只能删除自己的文章
            boolean deleted = articleService.deleteArticle(id, authenticatedUserId);
            if (deleted) {
                return ResponseEntity.ok("文章删除成功");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未找到该文章或无权删除，删除失败");
            }
        } catch (Exception e) {
            logger.error("删除文章失败，文章ID为 {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("服务器错误");
        }
    }

}
