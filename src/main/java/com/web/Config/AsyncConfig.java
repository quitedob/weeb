package com.web.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor; // For CallerRunsPolicy

/**
 * 异步线程池配置
 * 简化注释：异步配置
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer { // Modified to implement AsyncConfigurer

    @Override // Overrides AsyncConfigurer method
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 核心线程数
        executor.setMaxPoolSize(20); // 最大线程数
        executor.setQueueCapacity(200); // 任务队列容量 (updated from 100 to 200)
        executor.setThreadNamePrefix("async-task-"); // 线程名前缀

        // Set RejectedExecutionHandler
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    // The old taskExecutor() bean method is effectively replaced by getAsyncExecutor()
    // when implementing AsyncConfigurer for the default async executor.
    // If a bean named "taskExecutor" is still explicitly needed elsewhere,
    // another @Bean method could be defined, possibly returning this.getAsyncExecutor().
    // For now, assuming this default configuration is the primary goal.
}
