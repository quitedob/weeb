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
        executor.setMaxPoolSize(50); // 最大线程数 (增加以处理更高并发)
        executor.setQueueCapacity(500); // 任务队列容量 (增加缓冲区)
        executor.setThreadNamePrefix("async-task-"); // 线程名前缀

        // 拒绝策略：CallerRunsPolicy - 当线程池满时，由调用线程执行任务，避免任务丢失
        // 这样可以保证任务得到执行，但可能会影响调用线程的响应时间
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 设置线程存活时间，避免空闲线程占用资源
        executor.setKeepAliveSeconds(60);

        executor.initialize();
        return executor;
    }

    // The old taskExecutor() bean method is effectively replaced by getAsyncExecutor()
    // when implementing AsyncConfigurer for the default async executor.
    // If a bean named "taskExecutor" is still explicitly needed elsewhere,
    // another @Bean method could be defined, possibly returning this.getAsyncExecutor().
    // For now, assuming this default configuration is the primary goal.
}
