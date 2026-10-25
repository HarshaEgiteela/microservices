
package com.example.productservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory
    ) {

        // Jackson 3 JSON serializer
        GenericJacksonJsonRedisSerializer serializer =
                new GenericJacksonJsonRedisSerializer(
                        JsonMapper.builder().build()
                );

        // Redis cache configuration
        RedisCacheConfiguration config =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(5))
                        .serializeValuesWith(
                                RedisSerializationContext
                                        .SerializationPair
                                        .fromSerializer(serializer)
                        )
                        .disableCachingNullValues();

        // Redis cache manager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }
}
