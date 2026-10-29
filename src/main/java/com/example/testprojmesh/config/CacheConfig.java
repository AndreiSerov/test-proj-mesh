package com.example.testprojmesh.config;

import com.example.testprojmesh.service.model.PageView;
import com.example.testprojmesh.service.model.ProfileView;
import com.example.testprojmesh.service.model.UserSummary;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis caches for read-mostly User data (ADR 0004). Money is never cached: Balances change on every Transfer and
 * every Interest accrual. Values are JSON with one known type per cache, so no polymorphic type hints are stored.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /** Another User's profile without the Balance, by user id; evicted on that User's Contact writes. */
    public static final String USER_PROFILES = "userProfiles";

    /** Search result pages; emptied on any Contact write, since one change can move a User across pages. */
    public static final String USER_SEARCH = "userSearch";

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory factory, JsonMapper json) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));
        JavaType searchPage = json.getTypeFactory().constructParametricType(PageView.class, UserSummary.class);
        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaults)
            .withInitialCacheConfigurations(Map.of(
                USER_PROFILES, defaults.entryTtl(Duration.ofMinutes(10))
                    .serializeValuesWith(SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(json, ProfileView.class))),
                USER_SEARCH, defaults.entryTtl(Duration.ofMinutes(1))
                    .serializeValuesWith(SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<PageView<UserSummary>>(json, searchPage)))))
            // puts and evictions inside a transaction run after it commits
            .transactionAware()
            .build();
    }

    /** A cache failure is a slow request, never a 500. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                warn("GET", cache, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                warn("PUT", cache, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                warn("EVICT", cache, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                warn("CLEAR", cache, e);
            }

            private void warn(String operation, Cache cache, RuntimeException e) {
                log.warn("Cache {} on {} failed, proceeding without cache: {}", operation, cache.getName(),
                    e.toString());
            }
        };
    }
}
