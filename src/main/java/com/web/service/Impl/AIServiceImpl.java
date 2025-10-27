package com.web.service.Impl;

import com.web.service.AIService;
import com.web.vo.ai.ChatRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 服务实现类
 * 提供各种 AI 功能服务的基础实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = true)
public class AIServiceImpl implements AIService {

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Value("${ai.provider:deepseek}")
    private String aiProvider;

    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${ai.deepseek.chat-model:deepseek-chat}")
    private String deepseekChatModel;

    @Value("${ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ai.ollama.chat-model:llama2}")
    private String ollamaChatModel;

    @Override
    public String generateArticleSummary(String content, int maxLength) {
        try {
            log.debug("生成文章摘要: contentLength={}, maxLength={}, provider={}", content.length(), maxLength, aiProvider);

            if (content == null || content.trim().isEmpty()) {
                return "";
            }

            // 准备AI请求
            Map<String, Object> request = new HashMap<>();
            request.put("model", deepseekChatModel);
            request.put("messages", List.of(
                Map.of(
                    "role", "user",
                    "content", "请为以下内容生成一个简洁的摘要，摘要长度不超过" + maxLength + "个字符：\n\n" + content
                )
            ));
            request.put("stream", false);
            request.put("temperature", 0.3);
            request.put("max_tokens", Math.min(maxLength / 2, 1000));

            // 调用DeepSeek API
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + deepseekApiKey);
            headers.put("Content-Type", "application/json");

            Map<String, Object> response = restTemplate.postForObject(
                deepseekBaseUrl + "/chat/completions",
                request,
                Map.class,
                headers
            );

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null && message.containsKey("content")) {
                        String summary = (String) message.get("content");
                        log.info("AI文章摘要生成成功: summaryLength={}", summary.length());
                        return summary;
                    }
                }
            }

            log.warn("AI文章摘要生成失败: response={}", response);
            // 降级到简单摘要
            return generateSimpleSummary(content, maxLength);

        } catch (Exception e) {
            log.error("调用AI API生成文章摘要失败: provider={}, error={}", aiProvider, e.getMessage(), e);
            // 降级到简单摘要
            return generateSimpleSummary(content, maxLength);
        }
    }

    /**
     * 生成简单摘要（降级方案）
     */
    private String generateSimpleSummary(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String[] sentences = content.split("[。！？.!?]");
        StringBuilder summary = new StringBuilder();

        for (String sentence : sentences) {
            if (summary.length() + sentence.length() > maxLength) {
                break;
            }
            if (sentence.trim().length() > 0) {
                summary.append(sentence.trim()).append("。");
            }
        }

        return summary.length() > 0 ? summary.toString() : content.substring(0, Math.min(maxLength, content.length()));
    }

    @Override
    public String refineText(String content, String tone) {
        try {
            log.debug("润色文本: tone={}", tone);

            if (content == null || content.trim().isEmpty()) {
                return content;
            }

            // 简单的文本润色实现（实际项目中应调用AI API）
            String refined = content.trim();

            switch (tone.toLowerCase()) {
                case "professional":
                    refined = refined.replace("很好", "优秀");
                    refined = refined.replace("不错", "良好");
                    break;
                case "casual":
                    refined = refined.replace("因此", "所以");
                    refined = refined.replace("此外", "而且");
                    break;
                case "formal":
                    refined = refined.replace("东西", "物品");
                    refined = refined.replace("搞定", "完成");
                    break;
                case "friendly":
                    refined = refined.replace("您", "你");
                    refined = refined.replace("请", "");
                    break;
            }

            return refined;
        } catch (Exception e) {
            log.error("润色文本失败", e);
            return content;
        }
    }

    @Override
    public List<String> generateTitleSuggestions(String content, int count) {
        try {
            log.debug("生成标题建议: contentLength={}, count={}", content.length(), count);

            List<String> suggestions = new ArrayList<>();

            if (content == null || content.trim().isEmpty()) {
                return suggestions;
            }

            // 简单的标题生成实现（实际项目中应调用AI API）
            String[] words = content.split("\\s+");
            for (int i = 0; i < Math.min(count, words.length) && i < 5; i++) {
                String title = words[i] + "相关内容";
                suggestions.add(title);
            }

            return suggestions;
        } catch (Exception e) {
            log.error("生成标题建议失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String chatWithAI(String message, String sessionId) {
        try {
            log.debug("AI聊天对话: sessionId={}", sessionId);

            if (message == null || message.trim().isEmpty()) {
                return "请输入有效的问题。";
            }

            // 简单的聊天实现（实际项目中应调用AI API）
            if (message.contains("你好") || message.contains("hi")) {
                return "你好！有什么可以帮助您的吗？";
            } else if (message.contains("谢谢")) {
                return "不客气！还有其他问题吗？";
            } else {
                return "感谢您的提问，我正在学习中，目前无法提供详细的回答。";
            }
        } catch (Exception e) {
            log.error("AI聊天对话失败", e);
            return "抱歉，服务暂时不可用，请稍后再试。";
        }
    }

    @Override
    public String chat(ChatRequestVo requestVo, Long userId) {
        try {
            log.debug("AI通用聊天: userId={}, stream={}", userId, requestVo.isStream());

            if (requestVo == null || requestVo.getMessages() == null || requestVo.getMessages().isEmpty()) {
                return "请提供有效的对话内容。";
            }

            // 获取最后一条用户消息
            String lastMessage = "";
            for (ChatRequestVo.MessageVo msg : requestVo.getMessages()) {
                if ("user".equals(msg.getRole())) {
                    lastMessage = msg.getContent();
                }
            }

            return chatWithAI(lastMessage, String.valueOf(userId));
        } catch (Exception e) {
            log.error("AI通用聊天失败", e);
            return "抱歉，聊天服务暂时不可用。";
        }
    }

    @Override
    public Map<String, Object> analyzeSentiment(String content) {
        try {
            log.debug("分析内容情感");

            Map<String, Object> result = new HashMap<>();
            result.put("sentiment", "neutral");
            result.put("confidence", 0.5);
            result.put("positive", 0.3);
            result.put("negative", 0.3);
            result.put("neutral", 0.4);

            if (content != null) {
                String lowerContent = content.toLowerCase();
                if (lowerContent.contains("好") || lowerContent.contains("棒") || lowerContent.contains("赞")) {
                    result.put("sentiment", "positive");
                    result.put("confidence", 0.8);
                } else if (lowerContent.contains("差") || lowerContent.contains("坏") || lowerContent.contains("糟")) {
                    result.put("sentiment", "negative");
                    result.put("confidence", 0.8);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("分析内容情感失败", e);
            return Map.of("error", "情感分析失败");
        }
    }

    @Override
    public List<String> extractKeywords(String content, int count) {
        try {
            log.debug("提取关键词: count={}", count);

            List<String> keywords = new ArrayList<>();

            if (content == null || content.trim().isEmpty()) {
                return keywords;
            }

            // 简单的关键词提取（实际项目中应使用AI API）
            String[] words = content.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5\\s]", "").split("\\s+");
            Set<String> uniqueWords = new LinkedHashSet<>();

            for (String word : words) {
                if (word.length() > 1) {
                    uniqueWords.add(word);
                }
            }

            keywords.addAll(uniqueWords);
            return keywords.subList(0, Math.min(count, keywords.size()));
        } catch (Exception e) {
            log.error("提取关键词失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String translateText(String content, String targetLanguage) {
        try {
            log.debug("翻译文本: targetLanguage={}", targetLanguage);

            if (content == null || content.trim().isEmpty()) {
                return content;
            }

            // 简单的翻译实现（实际项目中应调用AI API）
            Map<String, String> translations = Map.of(
                "english", "This is a translated text.",
                "chinese", "这是翻译后的文本。",
                "japanese", "これは翻訳されたテキストです。"
            );

            return translations.getOrDefault(targetLanguage.toLowerCase(), "翻译功能暂未实现。");
        } catch (Exception e) {
            log.error("翻译文本失败", e);
            return "翻译失败，请稍后再试。";
        }
    }

    @Override
    public List<String> generateArticleTags(String content, int count) {
        try {
            log.debug("生成文章标签: count={}", count);

            List<String> tags = new ArrayList<>();

            if (content == null || content.trim().isEmpty()) {
                return tags;
            }

            // 简单的标签生成（实际项目中应调用AI API）
            String[] words = content.split("\\s+");
            Set<String> uniqueWords = new LinkedHashSet<>();

            for (String word : words) {
                if (word.length() > 2 && word.length() < 10) {
                    uniqueWords.add(word);
                }
            }

            tags.addAll(uniqueWords);
            return tags.subList(0, Math.min(count, tags.size()));
        } catch (Exception e) {
            log.error("生成文章标签失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> checkContentCompliance(String content) {
        try {
            log.debug("检查内容合规性");

            Map<String, Object> result = new HashMap<>();
            result.put("compliant", true);
            result.put("risk", "low");
            result.put("issues", new ArrayList<>());

            if (content != null) {
                String lowerContent = content.toLowerCase();
                List<String> issues = new ArrayList<>();

                // 简单的合规性检查
                if (lowerContent.contains("违法") || lowerContent.contains("暴力")) {
                    issues.add("内容可能包含不当信息");
                    result.put("compliant", false);
                    result.put("risk", "high");
                }

                result.put("issues", issues);
            }

            return result;
        } catch (Exception e) {
            log.error("检查内容合规性失败", e);
            return Map.of("error", "合规性检查失败");
        }
    }

    @Override
    public List<String> generateReplySuggestions(String originalMessage, String context) {
        try {
            log.debug("生成回复建议");

            List<String> suggestions = new ArrayList<>();

            if (originalMessage == null || originalMessage.trim().isEmpty()) {
                return suggestions;
            }

            // 简单的回复建议生成
            suggestions.add("感谢您的消息！");
            suggestions.add("我理解您的意思。");
            suggestions.add("让我想想这个问题。");

            return suggestions;
        } catch (Exception e) {
            log.error("生成回复建议失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String summarizeConversation(List<Map<String, Object>> messages, int maxLength) {
        try {
            log.debug("总结对话历史: messageCount={}, maxLength={}", messages.size(), maxLength);

            if (messages == null || messages.isEmpty()) {
                return "";
            }

            StringBuilder summary = new StringBuilder();
            int messageCount = 0;

            for (Map<String, Object> message : messages) {
                if (messageCount >= 5) break; // 限制总结的消息数量

                String content = (String) message.get("content");
                if (content != null && content.trim().length() > 0) {
                    if (summary.length() + content.length() > maxLength) {
                        break;
                    }
                    summary.append(content.trim()).append(" ");
                    messageCount++;
                }
            }

            return summary.toString().trim();
        } catch (Exception e) {
            log.error("总结对话历史失败", e);
            return "对话总结生成失败。";
        }
    }

    @Override
    public Map<String, Object> generateContentSuggestions(String topic, String contentType) {
        try {
            log.debug("生成内容创作建议: topic={}, contentType={}", topic, contentType);

            Map<String, Object> suggestions = new HashMap<>();
            suggestions.put("topic", topic);
            suggestions.put("contentType", contentType);
            suggestions.put("suggestions", List.of(
                "建议从基础概念开始介绍",
                "添加具体的例子和案例",
                "包含实际应用场景"
            ));
            suggestions.put("outline", List.of(
                "引言",
                "主要内容",
                "总结"
            ));

            return suggestions;
        } catch (Exception e) {
            log.error("生成内容创作建议失败", e);
            return Map.of("error", "内容建议生成失败");
        }
    }

    @Override
    public Map<String, Object> proofreadText(String content) {
        try {
            log.debug("校对和修正文本");

            Map<String, Object> result = new HashMap<>();
            result.put("original", content);
            result.put("corrected", content);
            result.put("errors", new ArrayList<>());
            result.put("suggestions", List.of("内容看起来不错！"));

            if (content != null) {
                List<String> errors = new ArrayList<>();
                String corrected = content;

                // 简单的文本检查
                if (content.contains("的的")) {
                    errors.add("重复的'的'字");
                    corrected = corrected.replace("的的", "的");
                }

                result.put("corrected", corrected);
                result.put("errors", errors);
            }

            return result;
        } catch (Exception e) {
            log.error("校对和修正文本失败", e);
            return Map.of("error", "文本校对失败");
        }
    }

    @Override
    public Map<String, Object> generateContentOutline(String topic, String structure) {
        try {
            log.debug("生成内容大纲: topic={}, structure={}", topic, structure);

            Map<String, Object> outline = new HashMap<>();
            outline.put("topic", topic);
            outline.put("structure", structure);

            List<Map<String, Object>> sections = new ArrayList<>();
            sections.add(Map.of("title", "引言", "description", "介绍主题背景"));
            sections.add(Map.of("title", "主体内容", "description", "详细阐述主要内容"));
            sections.add(Map.of("title", "结论", "description", "总结要点"));

            outline.put("sections", sections);
            outline.put("estimatedLength", "1000-1500字");

            return outline;
        } catch (Exception e) {
            log.error("生成内容大纲失败", e);
            return Map.of("error", "大纲生成失败");
        }
    }
}