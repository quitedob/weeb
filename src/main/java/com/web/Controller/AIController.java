package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.AIService;
import com.web.util.ApiResponseUtil;
import com.web.Config.AIConfig;
import com.web.vo.ai.ArticleSummaryRequestVo;
import com.web.vo.ai.ArticleTagsGenerationRequestVo;
import com.web.vo.ai.ChatRequestVo;
import com.web.vo.ai.ContentComplianceCheckRequestVo;
import com.web.vo.ai.ContentOutlineRequestVo;
import com.web.vo.ai.ContentSuggestionsRequestVo;
import com.web.vo.ai.ConversationSummaryRequestVo;
import com.web.vo.ai.KeywordsExtractionRequestVo;
import com.web.vo.ai.ReplySuggestionsRequestVo;
import com.web.vo.ai.SentimentAnalysisRequestVo;
import com.web.vo.ai.TextProofreadRequestVo;
import com.web.vo.ai.TextRefineRequestVo;
import com.web.vo.ai.TextTranslationRequestVo;
import com.web.vo.ai.TitleSuggestionRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<List<String>>> generateTitleSuggestions(
            @RequestBody @Valid TitleSuggestionRequestVo request) {
        try {
            String content = request.getContent();
            Integer count = request.getCount();

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeSentiment(
            @RequestBody @Valid SentimentAnalysisRequestVo request) {
        try {
            String content = request.getContent();
            // Use VO validation and parameters, but call existing service method
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
    public ResponseEntity<ApiResponse<List<String>>> extractKeywords(
            @RequestBody @Valid KeywordsExtractionRequestVo request) {
        try {
            String content = request.getContent();
            Integer count = request.getCount();

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
    public ResponseEntity<ApiResponse<String>> translateText(
            @RequestBody @Valid TextTranslationRequestVo request) {
        try {
            String content = request.getContent();
            String targetLanguage = request.getTargetLanguage();

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
    public ResponseEntity<ApiResponse<List<String>>> generateArticleTags(
            @RequestBody @Valid ArticleTagsGenerationRequestVo request) {
        try {
            String content = request.getContent();
            Integer count = request.getCount();

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkContentCompliance(
            @RequestBody @Valid ContentComplianceCheckRequestVo request) {
        try {
            String content = request.getContent();

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
    public ResponseEntity<ApiResponse<List<String>>> generateReplySuggestions(
            @RequestBody @Valid ReplySuggestionsRequestVo request) {
        try {
            String originalMessage = request.getOriginalMessage();
            String context = request.getContext();

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
    public ResponseEntity<ApiResponse<String>> summarizeConversation(
            @RequestBody @Valid ConversationSummaryRequestVo request) {
        try {
            List<ConversationSummaryRequestVo.ConversationMessage> messages = request.getMessages();
            Integer maxLength = request.getMaxLength();

            if (messages == null || messages.isEmpty()) {
                return ApiResponseUtil.badRequestString("对话消息不能为空");
            }

            // Convert ConversationMessage to Map for AI service
            List<Map<String, Object>> messageMaps = messages.stream()
                .map(msg -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", msg.getRole());
                    map.put("content", msg.getContent());
                    map.put("timestamp", msg.getTimestamp());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());

            String summary = aiService.summarizeConversation(messageMaps, maxLength);
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateContentSuggestions(
            @RequestBody @Valid ContentSuggestionsRequestVo request) {
        try {
            String topic = request.getTopic();
            String contentType = request.getContentType();

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> proofreadText(
            @RequestBody @Valid TextProofreadRequestVo request) {
        try {
            String content = request.getContent();

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateContentOutline(
            @RequestBody @Valid ContentOutlineRequestVo request) {
        try {
            String topic = request.getTopic();
            String structure = request.getStructure();

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