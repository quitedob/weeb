package com.web.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.Config.AiProperties;
import com.web.service.AIService;
import com.web.vo.ai.ChatRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI 服务实现类
 * 基于 Spring AI 框架实现各种 AI 功能
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false) // Spring AI is optional now
    private ChatClient chatClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ai.chat.context.ttl:30}")
    private int chatContextTtlMinutes;

    @Value("${ai.rate.limit.requests:100}")
    private int rateLimitRequests;

    @Value("${ai.rate.limit.window:3600}")
    private int rateLimitWindowSeconds;

    @Override
    public String chat(ChatRequestVo requestVo, Long userId) {
        String provider = aiProperties.getProvider();

        if ("ollama".equalsIgnoreCase(provider)) {
            return chatWithOllama(requestVo);
        } else if ("deepseek".equalsIgnoreCase(provider)) {
            return chatWithDeepSeek(requestVo);
        } else {
            // Fallback to default Spring AI provider if configured and no specific provider matches
            if (chatClient != null) {
                log.warn("未知的 AI 提供商 '{}'，回退到默认的 Spring AI ChatClient", provider);
                String sessionId = "session_for_user_" + userId;
                String lastMessage = requestVo.getMessages().get(requestVo.getMessages().size() - 1).getContent();
                return chatWithAI(lastMessage, sessionId);
            }
            throw new IllegalStateException("AI provider not configured correctly. Please check 'ai.provider' in application.yml");
        }
    }

    private String chatWithOllama(ChatRequestVo requestVo) {
        String url = aiProperties.getOllama().getBaseUrl() + "/api/chat";
        
        // 构建符合Ollama API的请求体
        Map<String, Object> ollamaRequest = new HashMap<>();
        ollamaRequest.put("model", aiProperties.getOllama().getChatModel());
        ollamaRequest.put("messages", requestVo.getMessages());
        ollamaRequest.put("stream", false); // 强制非流式

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ollamaRequest);
        
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
        if (response != null && response.get("message") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> message = (Map<String, String>) response.get("message");
            return message.get("content");
        }
        return "从Ollama获取响应失败";
    }

    private String chatWithDeepSeek(ChatRequestVo requestVo) {
        String url = aiProperties.getDeepseek().getBaseUrl() + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getDeepseek().getApiKey());

        // 构建符合DeepSeek API的请求体
        Map<String, Object> deepseekRequest = new HashMap<>();
        deepseekRequest.put("model", aiProperties.getDeepseek().getChatModel());
        deepseekRequest.put("messages", requestVo.getMessages());
        deepseekRequest.put("stream", false); // 强制非流式

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(deepseekRequest, headers);
        
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.get("choices") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty() && choices.get(0).get("message") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> message = (Map<String, String>) choices.get(0).get("message");
                return message.get("content");
            }
        }
        return "从DeepSeek获取响应失败";
    }

    /**
     * 生成文章摘要
     */
    @Override
    public String generateArticleSummary(String content, int maxLength) {
        try {
            String prompt = String.format(
                    "请为以下文章生成一个简洁的摘要，最多%d个字符：\n\n%s",
                    maxLength, content
            );

            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("生成文章摘要失败", e);
            return "摘要生成失败，请稍后重试。";
        }
    }

    /**
     * 润色文本内容
     */
    @Override
    public String refineText(String content, String tone) {
        try {
            String toneDescription = getToneDescription(tone);
            String prompt = String.format(
                    "请将以下文本改写为%s的语气，保持原意不变：\n\n%s",
                    toneDescription, content
            );

            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("润色文本失败: tone={}", tone, e);
            return "文本润色失败，请稍后重试。";
        }
    }

    /**
     * 生成文章标题建议
     */
    @Override
    public List<String> generateTitleSuggestions(String content, int count) {
        try {
            String prompt = String.format(
                    "基于以下文章内容，请生成%d个吸引人的标题建议：\n\n%s\n\n" +
                    "请以编号列表的形式返回，每行一个标题：",
                    count, content
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseTitleSuggestions(response);
        } catch (Exception e) {
            log.error("生成标题建议失败", e);
            return Arrays.asList("标题生成失败，请稍后重试。");
        }
    }

    /**
     * AI 聊天对话
     */
    @Override
    public String chatWithAI(String message, String sessionId) {
        try {
            // 检查速率限制
            if (!checkRateLimit(sessionId)) {
                return "请求过于频繁，请稍后再试。";
            }

            // 获取或创建会话记忆
            InMemoryChatMemory chatMemory = getOrCreateChatMemory(sessionId);

            // 创建带记忆的聊天客户端
            ChatClient memoryChatClient = ChatClient.builder(chatClient.getChatModel())
                    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                    .build();

            return memoryChatClient.prompt()
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI聊天失败: sessionId={}", sessionId, e);
            return "AI暂时无法回复，请稍后重试。";
        }
    }

    /**
     * 分析内容情感
     */
    @Override
    public Map<String, Object> analyzeSentiment(String content) {
        try {
            String prompt = String.format(
                    "请分析以下文本的情感倾向，返回JSON格式结果：\n\n%s\n\n" +
                    "请包含以下字段：\n" +
                    "- sentiment: positive/negative/neutral\n" +
                    "- confidence: 0-1之间的置信度\n" +
                    "- emotions: 主要情感列表\n" +
                    "- explanation: 简短分析说明",
                    content
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseSentimentAnalysis(response);
        } catch (Exception e) {
            log.error("情感分析失败", e);
            return Map.of(
                    "sentiment", "neutral",
                    "confidence", 0.5,
                    "emotions", List.of("neutral"),
                    "explanation", "分析失败"
            );
        }
    }

    /**
     * 提取关键词
     */
    @Override
    public List<String> extractKeywords(String content, int count) {
        try {
            String prompt = String.format(
                    "请从以下文本中提取%d个最重要的关键词，以逗号分隔：\n\n%s",
                    count, content
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return Arrays.asList(response.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(keyword -> !keyword.isEmpty())
                    .limit(count)
                    .toList();
        } catch (Exception e) {
            log.error("关键词提取失败", e);
            return List.of("提取失败");
        }
    }

    /**
     * 翻译文本
     */
    @Override
    public String translateText(String content, String targetLanguage) {
        try {
            String prompt = String.format(
                    "请将以下文本翻译成%s：\n\n%s",
                    targetLanguage, content
            );

            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("文本翻译失败: targetLanguage={}", targetLanguage, e);
            return "翻译失败，请稍后重试。";
        }
    }

    /**
     * 生成文章标签
     */
    @Override
    public List<String> generateArticleTags(String content, int count) {
        try {
            String prompt = String.format(
                    "基于以下文章内容，请生成%d个相关的标签，以逗号分隔：\n\n%s",
                    count, content
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return Arrays.asList(response.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .limit(count)
                    .toList();
        } catch (Exception e) {
            log.error("生成文章标签失败", e);
            return List.of("AI", "文章");
        }
    }

    /**
     * 检查内容合规性
     */
    @Override
    public Map<String, Object> checkContentCompliance(String content) {
        try {
            String prompt = String.format(
                    "请检查以下内容是否合规，返回JSON格式结果：\n\n%s\n\n" +
                    "请包含以下字段：\n" +
                    "- isCompliant: true/false\n" +
                    "- riskLevel: low/medium/high\n" +
                    "- issues: 发现的问题列表\n" +
                    "- suggestions: 改进建议",
                    content
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseComplianceCheck(response);
        } catch (Exception e) {
            log.error("内容合规性检查失败", e);
            return Map.of(
                    "isCompliant", true,
                    "riskLevel", "unknown",
                    "issues", List.of(),
                    "suggestions", List.of("无法检查合规性")
            );
        }
    }

    /**
     * 生成回复建议
     */
    @Override
    public List<String> generateReplySuggestions(String originalMessage, String context) {
        try {
            String prompt = String.format(
                    "基于原始消息和上下文，请生成3个合适的回复建议：\n\n" +
                    "原始消息：%s\n\n" +
                    "上下文：%s\n\n" +
                    "请以编号列表的形式返回，每行一个回复建议：",
                    originalMessage, context
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseReplySuggestions(response);
        } catch (Exception e) {
            log.error("生成回复建议失败", e);
            return List.of("好的", "我明白了", "谢谢分享");
        }
    }

    /**
     * 总结对话历史
     */
    @Override
    public String summarizeConversation(List<Map<String, Object>> messages, int maxLength) {
        try {
            StringBuilder conversation = new StringBuilder();
            for (Map<String, Object> message : messages) {
                String role = (String) message.get("role");
                String content = (String) message.get("content");
                if (role != null && content != null) {
                    conversation.append(role).append(": ").append(content).append("\n");
                }
            }

            String prompt = String.format(
                    "请总结以下对话内容，最多%d个字符：\n\n%s",
                    maxLength, conversation.toString()
            );

            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("对话总结失败", e);
            return "对话总结失败。";
        }
    }

    /**
     * 生成内容创作建议
     */
    @Override
    public Map<String, Object> generateContentSuggestions(String topic, String contentType) {
        try {
            String prompt = String.format(
                    "请为关于'%s'的%s类型内容提供建议，返回JSON格式：\n\n" +
                    "请包含以下字段：\n" +
                    "- title: 建议标题\n" +
                    "- outline: 内容大纲\n" +
                    "- keywords: 关键词列表\n" +
                    "- targetAudience: 目标受众\n" +
                    "- tips: 创作技巧",
                    topic, contentType
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseContentSuggestions(response);
        } catch (Exception e) {
            log.error("生成内容创作建议失败", e);
            return Map.of(
                    "title", "内容标题",
                    "outline", "内容大纲",
                    "keywords", List.of("关键词1", "关键词2"),
                    "targetAudience", "目标受众",
                    "tips", List.of("创作技巧")
            );
        }
    }

    /**
     * 校对和修正文本
     */
    @Override
    public Map<String, Object> proofreadText(String content) {
        try {
            String prompt = String.format(
                    "请校对以下文本并提供修正建议，返回JSON格式：\n\n%s\n\n" +
                    "请包含以下字段：\n" +
                    "- correctedText: 修正后的文本\n" +
                    "- errors: 发现的错误列表\n" +
                    "- suggestions: 改进建议\n" +
                    "- score: 质量评分(0-10)",
                    content
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseProofreadResult(response);
        } catch (Exception e) {
            log.error("文本校对失败", e);
            return Map.of(
                    "correctedText", content,
                    "errors", List.of(),
                    "suggestions", List.of("无建议"),
                    "score", 10
            );
        }
    }

    /**
     * 生成内容大纲
     */
    @Override
    public Map<String, Object> generateContentOutline(String topic, String structure) {
        try {
            String prompt = String.format(
                    "请为'%s'主题生成%s结构的内容大纲，返回JSON格式：\n\n" +
                    "请包含以下字段：\n" +
                    "- title: 内容标题\n" +
                    "- sections: 章节列表，每个章节包含title和content\n" +
                    "- estimatedLength: 预估字数\n" +
                    "- targetWordCount: 目标字数",
                    topic, structure
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseContentOutline(response);
        } catch (Exception e) {
            log.error("生成内容大纲失败", e);
            return Map.of(
                    "title", topic,
                    "sections", List.of(),
                    "estimatedLength", 1000,
                    "targetWordCount", 1000
            );
        }
    }

    // 私有辅助方法

    private String getToneDescription(String tone) {
        Map<String, String> toneMap = Map.of(
                "professional", "专业、正式",
                "casual", "轻松、随意",
                "formal", "正式、庄重",
                "friendly", "友好、亲切",
                "academic", "学术、严谨",
                "creative", "创意、生动"
        );
        return toneMap.getOrDefault(tone.toLowerCase(), tone);
    }

    private List<String> parseTitleSuggestions(String response) {
        return Arrays.stream(response.split("\n"))
                .filter(line -> line.matches("\\d+\\..*"))
                .map(line -> line.replaceFirst("^\\d+\\.\\s*", ""))
                .limit(5)
                .toList();
    }

    private Map<String, Object> parseSentimentAnalysis(String response) {
        try {
            // 使用 Jackson 解析 JSON 响应
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析情感分析JSON响应失败: {}", response, e);
            // 解析失败，返回一个包含错误信息的默认结果
            return Map.of(
                    "sentiment", "unknown",
                    "confidence", 0.0,
                    "emotions", List.of(),
                    "explanation", "Failed to parse AI response.",
                    "rawResponse", response
            );
        }
    }

    private Map<String, Object> parseComplianceCheck(String response) {
        try {
            // 使用 Jackson 解析 JSON 响应
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析合规性检查JSON响应失败: {}", response, e);
            // 解析失败，返回一个包含错误信息的默认结果
            return Map.of(
                    "isCompliant", false,
                    "riskLevel", "unknown",
                    "issues", List.of("Failed to parse AI response."),
                    "suggestions", List.of(),
                    "rawResponse", response
            );
        }
    }

    private List<String> parseReplySuggestions(String response) {
        return Arrays.stream(response.split("\n"))
                .filter(line -> line.matches("\\d+\\..*"))
                .map(line -> line.replaceFirst("^\\d+\\.\\s*", ""))
                .limit(3)
                .toList();
    }

    private Map<String, Object> parseContentSuggestions(String response) {
        try {
            // 使用 Jackson 解析 JSON 响应
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析内容建议JSON响应失败: {}", response, e);
            // 解析失败，返回一个包含错误信息的默认结果
            return Map.of(
                    "title", "生成失败",
                    "outline", "生成失败",
                    "keywords", List.of(),
                    "targetAudience", "未知",
                    "tips", List.of("解析AI响应失败"),
                    "rawResponse", response
            );
        }
    }

    private Map<String, Object> parseProofreadResult(String response) {
        try {
            // 使用 Jackson 解析 JSON 响应
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析文本校对JSON响应失败: {}", response, e);
            // 解析失败，返回一个包含错误信息的默认结果
            return Map.of(
                    "correctedText", response,
                    "errors", List.of("解析AI响应失败"),
                    "suggestions", List.of(),
                    "score", 0,
                    "rawResponse", response
            );
        }
    }

    private Map<String, Object> parseContentOutline(String response) {
        try {
            // 使用 Jackson 解析 JSON 响应
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析内容大纲JSON响应失败: {}", response, e);
            // 解析失败，返回一个包含错误信息的默认结果
            return Map.of(
                    "title", "生成失败",
                    "sections", List.of(),
                    "estimatedLength", 0,
                    "targetWordCount", 0,
                    "rawResponse", response
            );
        }
    }

    private InMemoryChatMemory getOrCreateChatMemory(String sessionId) {
        // 从Redis获取或创建会话记忆
        String key = "ai:chat:memory:" + sessionId;
        InMemoryChatMemory memory = (InMemoryChatMemory) redisTemplate.opsForValue().get(key);

        if (memory == null) {
            memory = new InMemoryChatMemory();
            redisTemplate.opsForValue().set(key, memory, Duration.ofMinutes(chatContextTtlMinutes));
        }

        return memory;
    }

    private boolean checkRateLimit(String sessionId) {
        String key = "ai:rate:limit:" + sessionId;
        String requestsKey = key + ":requests";

        try {
            Long currentRequests = redisTemplate.opsForValue().increment(requestsKey);
            if (currentRequests == 1) {
                redisTemplate.expire(requestsKey, Duration.ofSeconds(rateLimitWindowSeconds));
            }

            return currentRequests <= rateLimitRequests;
        } catch (Exception e) {
            log.error("检查速率限制失败", e);
            return true; // 出错时允许请求
        }
    }
}