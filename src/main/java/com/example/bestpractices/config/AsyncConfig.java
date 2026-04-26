package com.example.bestpractices.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Best practices demonstrated:
 * - Implement AsyncConfigurer to provide a named thread pool (avoids the default SimpleAsyncTaskExecutor
 *   which creates a new thread per task and does not reuse threads)
 * - Size the pool based on the workload: CPU-bound → core count; IO-bound → higher
 * - Set a queue capacity limit to apply back-pressure instead of unbounded queuing
 * - ThreadNamePrefix helps correlate async work in logs
 * - Provide an AsyncUncaughtExceptionHandler so exceptions in void @Async methods are not silently swallowed
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Uncaught async exception in method '{}': {}", method.getName(), ex.getMessage(), ex);
    }
}
