package com.web.Config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator; // Correct import for enableDefaultTyping
import com.web.service.RedisSubscriber; // This class will be created in the next step
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    public static final String USER_MESSAGE_TOPIC = "user:messages";

    @Bean
    @SuppressWarnings("all") // To suppress warnings from Spring about generic types with RedisTemplate
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用新的构造函数方式创建序列化器，避免使用已弃用的setObjectMapper方法
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL); // Deprecated
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL); // Replacement for enableDefaultTyping

        // 直接使用带有ObjectMapper的构造函数
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) { // Assumes RedisSubscriber is injectable
        return new MessageListenerAdapter(subscriber, "handleMessage"); // Default method name "handleMessage"
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new ChannelTopic(USER_MESSAGE_TOPIC));
        // Consider adding a TaskExecutor for the container if message processing is intensive
        // org.springframework.core.task.SimpleAsyncTaskExecutor executor = new org.springframework.core.task.SimpleAsyncTaskExecutor();
        // executor.setConcurrencyLimit(10); // Example
        // container.setTaskExecutor(executor);
        return container;
    }
    
    /**
     * 自定义 Redis 健康指示器，避免 INFO 命令解析问题
     * 使用简单的 PING 命令代替 INFO 命令进行健康检查
     * 只有在启用健康检查时才创建此 Bean
     */
    @Bean
    @ConditionalOnProperty(name = "management.health.redis.enabled", havingValue = "true", matchIfMissing = false)
    public HealthIndicator redisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        return () -> {
            try {
                // 使用简单的 PING 命令代替 INFO 命令
                String response = (String) redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
                
                if ("PONG".equals(response)) {
                    return Health.up()
                        .withDetail("status", "Redis 连接正常")
                        .withDetail("response", response)
                        .build();
                } else {
                    return Health.down()
                        .withDetail("status", "Redis 响应异常")
                        .withDetail("response", response)
                        .build();
                }
            } catch (Exception e) {
                return Health.down()
                    .withDetail("status", "Redis 连接失败")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }
}
