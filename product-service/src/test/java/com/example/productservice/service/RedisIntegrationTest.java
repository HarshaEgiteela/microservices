
package com.example.productservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisIntegrationTest {

    @Container
    private static final GenericContainer<?> redis =
            new GenericContainer<>(
                    DockerImageName.parse("redis:7-alpine")
            ).withExposedPorts(6379);

    private StringRedisTemplate createTemplate() {

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(
                        redis.getHost(),
                        redis.getMappedPort(6379)
                );

        factory.afterPropertiesSet();

        StringRedisTemplate template =
                new StringRedisTemplate(factory);

        template.afterPropertiesSet();

        return template;
    }

    @Test
    void shouldStoreAndRetrieveValue() {

        StringRedisTemplate template = createTemplate();

        String key = "test:product:" + UUID.randomUUID();

        try {
            template.opsForValue().set(
                    key,
                    "Laptop",
                    Duration.ofSeconds(30)
            );

            String result = template.opsForValue().get(key);

            assertEquals("Laptop", result);

        } finally {
            template.delete(key);
            ((LettuceConnectionFactory)
                    template.getConnectionFactory()).destroy();
        }
    }

    @Test
    void shouldExpireCachedValue() throws InterruptedException {

        StringRedisTemplate template = createTemplate();

        String key = "test:ttl:" + UUID.randomUUID();

        try {
            template.opsForValue().set(
                    key,
                    "Temporary Product",
                    Duration.ofSeconds(2)
            );

            assertEquals(
                    "Temporary Product",
                    template.opsForValue().get(key)
            );

            Thread.sleep(3000);

            assertNull(
                    template.opsForValue().get(key)
            );

        } finally {
            template.delete(key);
            ((LettuceConnectionFactory)
                    template.getConnectionFactory()).destroy();
        }
    }
}
