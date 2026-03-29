package com.javaplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.context.annotation.Bean;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public org.springframework.cache.CacheManager cacheManager(
        org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.create(connectionFactory);
    }
}
