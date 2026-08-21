package com.example.bestpractices.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Best practices demonstrated:
 * - Redis replaces Caffeine for distributed caching — cache is shared across all
 *   nodes in a horizontally scaled deployment; Caffeine is limited to a single JVM
 * - Per-cache TTLs: user data cached for 10 min, aggregates for 5 min
 * - GenericJacksonJsonRedisSerializer stores human-readable JSON (not Java blobs),
 *   enabling inspection with redis-cli and compatibility with non-Java consumers
 * - disableCachingNullValues prevents a null response from poisoning the cache
 * - transactionAware() ensures @CacheEvict participates in Spring transactions:
 *   eviction is deferred until the transaction commits, preventing stale reads on rollback
 * - Cache names as constants eliminate typo-based silent cache misses
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USERS_CACHE = "users";
    public static final String USER_STATS_CACHE = "userStats";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJacksonJsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCacheTtls = Map.of(
                USERS_CACHE,      base.entryTtl(Duration.ofMinutes(10)),
                USER_STATS_CACHE, base.entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(perCacheTtls)
                .transactionAware()
                .build();
    }
}
