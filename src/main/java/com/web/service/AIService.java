package com.web.service;

import com.web.vo.ai.ChatRequestVo;

import java.util.List;
import java.util.Map;

/**
 * AI 服务接口
 * 提供各种 AI 功能服务
 */
public interface AIService {

    /**
     * 生成文章摘要
     * @param content 文章内容
     * @param maxLength 摘要最大长度
     * @return 生成的摘要
     */
    String generateArticleSummary(String content, int maxLength);

    /**
     * 润色文本内容
     * @param content 原始内容
     * @param tone 目标语气 (professional, casual, formal, friendly等)
     * @return 润色后的内容
     */
    String refineText(String content, String tone);

    /**
     * 生成文章标题建议
     * @param content 文章内容
     * @param count 建议数量
     * @return 标题建议列表
     */
    List<String> generateTitleSuggestions(String content, int count);

    /**
     * AI 聊天对话
     * @param message 用户消息
     * @param sessionId 会话ID (用于上下文管理)
     * @return AI 回复
     */
    String chatWithAI(String message, String sessionId);

    /**
     * 与AI进行通用聊天对话 (新)
     * @param requestVo 包含模型、消息历史和流式标志的请求对象
     * @param userId 当前用户ID
     * @return AI的回复
     */
    String chat(ChatRequestVo requestVo, Long userId);

    /**
     * 分析内容情感
     * @param content 文本内容
     * @return 情感分析结果
     */
    Map<String, Object> analyzeSentiment(String content);

    /**
     * 提取关键词
     * @param content 文本内容
     * @param count 关键词数量
     * @return 关键词列表
     */
    List<String> extractKeywords(String content, int count);

    /**
     * 翻译文本
     * @param content 原始文本
     * @param targetLanguage 目标语言
     * @return 翻译结果
     */
    String translateText(String content, String targetLanguage);

    /**
     * 生成文章标签
     * @param content 文章内容
     * @param count 标签数量
     * @return 标签列表
     */
    List<String> generateArticleTags(String content, int count);

    /**
     * 检查内容合规性
     * @param content 文本内容
     * @return 合规性检查结果
     */
    Map<String, Object> checkContentCompliance(String content);

    /**
     * 生成回复建议
     * @param originalMessage 原始消息
     * @param context 上下文信息
     * @return 回复建议
     */
    List<String> generateReplySuggestions(String originalMessage, String context);

    /**
     * 总结对话历史
     * @param messages 对话消息列表
     * @param maxLength 摘要最大长度
     * @return 对话摘要
     */
    String summarizeConversation(List<Map<String, Object>> messages, int maxLength);

    /**
     * 生成内容创作建议
     * @param topic 创作主题
     * @param contentType 内容类型 (article, blog, social等)
     * @return 创作建议
     */
    Map<String, Object> generateContentSuggestions(String topic, String contentType);

    /**
     * 校对和修正文本
     * @param content 待校对的文本
     * @return 校对结果和修正建议
     */
    Map<String, Object> proofreadText(String content);

    /**
     * 生成内容大纲
     * @param topic 主题
     * @param structure 结构要求
     * @return 内容大纲
     */
    Map<String, Object> generateContentOutline(String topic, String structure);
}