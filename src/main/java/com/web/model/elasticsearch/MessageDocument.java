package com.web.model.elasticsearch;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date; // Using java.util.Date as specified

/**
 * 消息在Elasticsearch中的文档模型
 * 简化注释：ES消息文档
 * 只有在启用Elasticsearch功能时才加载
 */
@Data
@Document(indexName = "message")
@ConditionalOnProperty(
    value = "elasticsearch.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class MessageDocument {

    @Id
    private Long id; // Corresponds to the message ID from the primary database

    @Field(type = FieldType.Long)
    private Long fromId; // Sender ID

    @Field(type = FieldType.Long)
    private Long chatListId; // Conversation/ChatList ID

    /**
     * 消息内容，指定分词器以支持中文搜索
     * 简化注释：消息内容
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content; // Actual text content of the message

    @Field(type = FieldType.Date)
    private Date sendTime; // Message send time
}
