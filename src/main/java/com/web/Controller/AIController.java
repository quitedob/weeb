package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.AIService;
import com.web.util.ApiResponseUtil;
import com.web.Config.AIConfig;
import com.web.vo.ai.ArticleSummaryRequestVo;
import com.web.vo.ai.ChatRequestVo;
import com.web.vo.ai.TextRefineRequestVo;
import com.web.vo.ai.TitleSuggestionRequestVo;
import com.web.vo.ai.SentimentAnalysisRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
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

    @Autowired
    private AIConfig aiConfig;

    /**
     * 生成文章摘要
     * POST /api/ai/article/summary
     */
    @PostMapping("/article/summary")
    @PreAuthorize("hasPermission(null, 'ARTICLE_READ_OWN')")
    public ResponseEntity<ApiResponse<String>> generateArticleSummary(
            @RequestBody @Valid ArticleSummaryRequestVo summaryRequest) {
        try {
            String content = summaryRequest.getContent();
            Integer maxLength = summaryRequest.getMaxLength();

            ApiResponseUtil.validateNotBlank(content, "文章内容");

            if (maxLength != null && maxLength < 10) {
                return ApiResponseUtil.badRequestString("摘要长度不能小于10个字符");
            }
            if (maxLength != null && maxLength > 1000) {
                return ApiResponseUtil.badRequestString("摘要长度不能超过1000个字符");
            }

            String summary = aiService.generateArticleSummary(content, maxLength);
            return ApiResponseUtil.success(summary, "摘要生成成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "生成文章摘要");
        }
    }

    /**
     * 润色文本内容
     * POST /api/ai/text/refine
     */
    @PostMapping("/text/refine")
    @PreAuthorize("hasPermission(null, 'ARTICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<String>> refineText(
            @RequestBody @Valid TextRefineRequestVo request) {
        try {
            String refinedText = aiService.refineText(request.getContent(), request.getTone());
            return ApiResponseUtil.success(refinedText, "文本润色成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "润色文本");
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

            ApiResponseUtil.validateNotBlank(content, "文章内容");

            List<String> titles = aiService.generateTitleSuggestions(content, count);
            return ApiResponseUtil.success(titles, "标题建议生成成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionList(e, "生成标题建议");
        }
    }

    /**
     * AI 聊天对话 (新的统一接口)
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    @PreAuthorize("hasPermission(null, 'AI_CHAT_OWN')")
    public ResponseEntity<ApiResponse<String>> chat(
            @RequestBody @Valid ChatRequestVo request,
            @Userid Long userId) {
        try {
            // 此处暂时保留简单实现，后续将在Service层扩展
            String lastMessageContent = request.getMessages().get(request.getMessages().size() - 1).getContent();
            String sessionId = "session_for_user_" + userId; // 示例 sessionId
            String response = aiService.chatWithAI(lastMessageContent, sessionId);
            return ApiResponseUtil.successString(response);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "AI聊天", userId);
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

            ApiResponseUtil.validateNotBlank(content, "分析内容");

            Map<String, Object> analysis = aiService.analyzeSentiment(content);
            return ApiResponseUtil.success(analysis, "情感分析完成");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "情感分析");
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

            ApiResponseUtil.validateNotBlank(content, "提取内容");

            List<String> keywords = aiService.extractKeywords(content, count);
            return ApiResponseUtil.success(keywords, "关键词提取成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionList(e, "关键词提取");
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

            ApiResponseUtil.validateNotBlank(content, "翻译内容");
            ApiResponseUtil.validateNotBlank(targetLanguage, "目标语言");

            String translation = aiService.translateText(content, targetLanguage);
            return ApiResponseUtil.success(translation, "翻译成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "文本翻译");
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

            ApiResponseUtil.validateNotBlank(content, "文章内容");

            List<String> tags = aiService.generateArticleTags(content, count);
            return ApiResponseUtil.success(tags, "标签生成成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionList(e, "生成文章标签");
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

            ApiResponseUtil.validateNotBlank(content, "检查内容");

            Map<String, Object> compliance = aiService.checkContentCompliance(content);
            return ApiResponseUtil.success(compliance, "合规性检查完成");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "内容合规性检查");
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

            ApiResponseUtil.validateNotBlank(originalMessage, "原始消息");

            List<String> suggestions = aiService.generateReplySuggestions(originalMessage, context);
            return ApiResponseUtil.success(suggestions, "回复建议生成成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionList(e, "生成回复建议");
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
                return ApiResponseUtil.badRequestString("对话消息不能为空");
            }

            String summary = aiService.summarizeConversation(messages, maxLength);
            return ApiResponseUtil.successString(summary);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "对话总结");
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

            ApiResponseUtil.validateNotBlank(topic, "主题");

            Map<String, Object> suggestions = aiService.generateContentSuggestions(topic, contentType);
            return ApiResponseUtil.success(suggestions, "创作建议生成成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "生成内容创作建议");
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

            ApiResponseUtil.validateNotBlank(content, "校对内容");

            Map<String, Object> result = aiService.proofreadText(content);
            return ApiResponseUtil.success(result, "文本校对完成");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "文本校对");
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

            ApiResponseUtil.validateNotBlank(topic, "主题");

            Map<String, Object> outline = aiService.generateContentOutline(topic, structure);
            return ApiResponseUtil.success(outline, "内容大纲生成成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "生成内容大纲");
        }
    }

    /**
     * 获取AI配置信息（提供给前端使用）
     * GET /api/ai/config
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("provider", aiConfig.getAiProvider());
            config.put("model", aiConfig.getCurrentModel());
            // 注意：不返回API key，保持安全
            config.put("configured", true);
            return ApiResponseUtil.successMap(config, "获取AI配置成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取AI配置");
        }
    }
}