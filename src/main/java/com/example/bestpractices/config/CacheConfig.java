package com.example.bestpractices.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Best practices demonstrated:
 * - Use Caffeine (in-process) for single-instance apps; swap for Redis when scaling horizontally
 * - Always set a TTL — an unbounded cache is a memory leak waiting to happen
 * - Set maximumSize to cap memory usage under load
 * - Define cache names as constants so typos cause compile errors, not silent cache misses
 */
@Configuration
public class CacheConfig {

    public static final String USERS_CACHE = "users";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(USERS_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }
}
