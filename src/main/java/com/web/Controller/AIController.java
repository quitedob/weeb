package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 功能控制器
 * 提供各种 AI 服务接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * 生成文章摘要
     * POST /api/ai/article/summary
     */
    @PostMapping("/article/summary")
    @PreAuthorize("hasPermission(null, 'ARTICLE_READ_OWN')")
    public ResponseEntity<ApiResponse<String>> generateArticleSummary(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            Integer maxLength = (Integer) request.getOrDefault("maxLength", 200);

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文章内容不能为空"));
            }

            String summary = aiService.generateArticleSummary(content, maxLength);
            return ResponseEntity.ok(ApiResponse.success("摘要生成成功", summary));
        } catch (Exception e) {
            log.error("生成文章摘要失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("生成摘要失败: " + e.getMessage()));
        }
    }

    /**
     * 润色文本内容
     * POST /api/ai/text/refine
     */
    @PostMapping("/text/refine")
    @PreAuthorize("hasPermission(null, 'ARTICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<String>> refineText(
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            String tone = request.getOrDefault("tone", "professional");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文本内容不能为空"));
            }

            String refinedText = aiService.refineText(content, tone);
            return ResponseEntity.ok(ApiResponse.success("文本润色成功", refinedText));
        } catch (Exception e) {
            log.error("润色文本失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("润色失败: " + e.getMessage()));
        }
    }

    /**
     * 生成文章标题建议
     * POST /api/ai/article/titles
     */
    @PostMapping("/article/titles")
    @PreAuthorize("hasPermission(null, 'ARTICLE_CREATE_OWN')")
    public ResponseEntity<ApiResponse<List<String>>> generateTitleSuggestions(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            Integer count = (Integer) request.getOrDefault("count", 5);

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文章内容不能为空"));
            }

            List<String> titles = aiService.generateTitleSuggestions(content, count);
            return ResponseEntity.ok(ApiResponse.success("标题建议生成成功", titles));
        } catch (Exception e) {
            log.error("生成标题建议失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("生成标题建议失败: " + e.getMessage()));
        }
    }

    /**
     * AI 聊天对话
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    @PreAuthorize("hasPermission(null, 'AI_CHAT_OWN')")
    public ResponseEntity<ApiResponse<String>> chatWithAI(
            @RequestBody Map<String, String> request,
            @Userid Long userId) {
        try {
            String message = request.get("message");
            String sessionId = request.getOrDefault("sessionId", "default_" + userId);

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("消息内容不能为空"));
            }

            String response = aiService.chatWithAI(message, sessionId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("AI聊天失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("AI聊天失败: " + e.getMessage()));
        }
    }

    /**
     * 分析内容情感
     * POST /api/ai/sentiment/analyze
     */
    @PostMapping("/sentiment/analyze")
    @PreAuthorize("hasPermission(null, 'CONTENT_ANALYZE_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeSentiment(
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("分析内容不能为空"));
            }

            Map<String, Object> analysis = aiService.analyzeSentiment(content);
            return ResponseEntity.ok(ApiResponse.success("情感分析完成", analysis));
        } catch (Exception e) {
            log.error("情感分析失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("情感分析失败: " + e.getMessage()));
        }
    }

    /**
     * 提取关键词
     * POST /api/ai/keywords/extract
     */
    @PostMapping("/keywords/extract")
    @PreAuthorize("hasPermission(null, 'CONTENT_ANALYZE_OWN')")
    public ResponseEntity<ApiResponse<List<String>>> extractKeywords(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            Integer count = (Integer) request.getOrDefault("count", 10);

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("提取内容不能为空"));
            }

            List<String> keywords = aiService.extractKeywords(content, count);
            return ResponseEntity.ok(ApiResponse.success("关键词提取成功", keywords));
        } catch (Exception e) {
            log.error("关键词提取失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("关键词提取失败: " + e.getMessage()));
        }
    }

    /**
     * 翻译文本
     * POST /api/ai/text/translate
     */
    @PostMapping("/text/translate")
    @PreAuthorize("hasPermission(null, 'AI_TRANSLATE_OWN')")
    public ResponseEntity<ApiResponse<String>> translateText(
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            String targetLanguage = request.get("targetLanguage");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("翻译内容不能为空"));
            }

            if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("目标语言不能为空"));
            }

            String translation = aiService.translateText(content, targetLanguage);
            return ResponseEntity.ok(ApiResponse.success("翻译成功", translation));
        } catch (Exception e) {
            log.error("文本翻译失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("翻译失败: " + e.getMessage()));
        }
    }

    /**
     * 生成文章标签
     * POST /api/ai/article/tags
     */
    @PostMapping("/article/tags")
    @PreAuthorize("hasPermission(null, 'ARTICLE_CREATE_OWN')")
    public ResponseEntity<ApiResponse<List<String>>> generateArticleTags(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            Integer count = (Integer) request.getOrDefault("count", 5);

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文章内容不能为空"));
            }

            List<String> tags = aiService.generateArticleTags(content, count);
            return ResponseEntity.ok(ApiResponse.success("标签生成成功", tags));
        } catch (Exception e) {
            log.error("生成文章标签失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("生成标签失败: " + e.getMessage()));
        }
    }

    /**
     * 检查内容合规性
     * POST /api/ai/content/compliance
     */
    @PostMapping("/content/compliance")
    @PreAuthorize("hasPermission(null, 'CONTENT_MODERATE_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkContentCompliance(
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("检查内容不能为空"));
            }

            Map<String, Object> compliance = aiService.checkContentCompliance(content);
            return ResponseEntity.ok(ApiResponse.success("合规性检查完成", compliance));
        } catch (Exception e) {
            log.error("内容合规性检查失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("合规性检查失败: " + e.getMessage()));
        }
    }

    /**
     * 生成回复建议
     * POST /api/ai/reply/suggestions
     */
    @PostMapping("/reply/suggestions")
    @PreAuthorize("hasPermission(null, 'AI_CHAT_OWN')")
    public ResponseEntity<ApiResponse<List<String>>> generateReplySuggestions(
            @RequestBody Map<String, String> request) {
        try {
            String originalMessage = request.get("originalMessage");
            String context = request.getOrDefault("context", "");

            if (originalMessage == null || originalMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("原始消息不能为空"));
            }

            List<String> suggestions = aiService.generateReplySuggestions(originalMessage, context);
            return ResponseEntity.ok(ApiResponse.success("回复建议生成成功", suggestions));
        } catch (Exception e) {
            log.error("生成回复建议失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("生成回复建议失败: " + e.getMessage()));
        }
    }

    /**
     * 总结对话历史
     * POST /api/ai/conversation/summary
     */
    @PostMapping("/conversation/summary")
    @PreAuthorize("hasPermission(null, 'AI_CHAT_OWN')")
    public ResponseEntity<ApiResponse<String>> summarizeConversation(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
            Integer maxLength = (Integer) request.getOrDefault("maxLength", 200);

            if (messages == null || messages.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("对话消息不能为空"));
            }

            String summary = aiService.summarizeConversation(messages, maxLength);
            return ResponseEntity.ok(ApiResponse.success("对话总结成功", summary));
        } catch (Exception e) {
            log.error("对话总结失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("对话总结失败: " + e.getMessage()));
        }
    }

    /**
     * 生成内容创作建议
     * POST /api/ai/content/suggestions
     */
    @PostMapping("/content/suggestions")
    @PreAuthorize("hasPermission(null, 'ARTICLE_CREATE_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateContentSuggestions(
            @RequestBody Map<String, String> request) {
        try {
            String topic = request.get("topic");
            String contentType = request.getOrDefault("contentType", "article");

            if (topic == null || topic.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("主题不能为空"));
            }

            Map<String, Object> suggestions = aiService.generateContentSuggestions(topic, contentType);
            return ResponseEntity.ok(ApiResponse.success("创作建议生成成功", suggestions));
        } catch (Exception e) {
            log.error("生成内容创作建议失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("生成创作建议失败: " + e.getMessage()));
        }
    }

    /**
     * 校对和修正文本
     * POST /api/ai/text/proofread
     */
    @PostMapping("/text/proofread")
    @PreAuthorize("hasPermission(null, 'ARTICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> proofreadText(
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("校对内容不能为空"));
            }

            Map<String, Object> result = aiService.proofreadText(content);
            return ResponseEntity.ok(ApiResponse.success("文本校对完成", result));
        } catch (Exception e) {
            log.error("文本校对失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("文本校对失败: " + e.getMessage()));
        }
    }

    /**
     * 生成内容大纲
     * POST /api/ai/content/outline
     */
    @PostMapping("/content/outline")
    @PreAuthorize("hasPermission(null, 'ARTICLE_CREATE_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateContentOutline(
            @RequestBody Map<String, String> request) {
        try {
            String topic = request.get("topic");
            String structure = request.getOrDefault("structure", "introduction-body-conclusion");

            if (topic == null || topic.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("主题不能为空"));
            }

            Map<String, Object> outline = aiService.generateContentOutline(topic, structure);
            return ResponseEntity.ok(ApiResponse.success("内容大纲生成成功", outline));
        } catch (Exception e) {
            log.error("生成内容大纲失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("生成内容大纲失败: " + e.getMessage()));
        }
    }
}