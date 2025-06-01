package com.web.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步线程池配置类 (AsyncConfig)。
 * 配置全局的线程池，用于处理异步任务，提升系统的并发性能。
 */
@Configuration // 标注为配置类，表明该类包含Bean定义
public class AsyncConfig {

    /**
     * 定义一个名为 "taskExecutor" 的线程池任务执行器。
     * 该线程池用于执行异步任务，例如事件处理、定时任务等。
     *
     * @return 配置好的 ThreadPoolTaskExecutor 对象
     */
    @Bean(name = "taskExecutor") // 定义Bean，供Spring容器管理，名称为"taskExecutor"
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 设置核心线程数，即线程池创建时的默认线程数。
        // 核心线程会一直存活，即使它们处于空闲状态。
        executor.setCorePoolSize(10);

        // 设置最大线程数，即线程池允许创建的最大线程数。
        // 当队列满且当前线程数小于最大线程数时，会创建新线程。
        executor.setMaxPoolSize(20);

        // 设置队列容量。
        // 当任务数量超过核心线程数时，任务会被放入队列中等待执行。
        executor.setQueueCapacity(100);

        // 设置线程名称前缀，方便在日志中识别线程来源。
        executor.setThreadNamePrefix("async-task-");

        // 初始化线程池。
        executor.initialize();
        return executor;
    }
}
