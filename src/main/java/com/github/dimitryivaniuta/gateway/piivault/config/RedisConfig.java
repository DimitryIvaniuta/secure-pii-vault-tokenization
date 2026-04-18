package com.github.dimitryivaniuta.gateway.piivault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

/**
 * Redis configuration for non-sensitive token state caching.
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a reactive string Redis template.
     *
     * @param connectionFactory reactive connection factory
     * @return template for string operations
     */
    @Bean
    ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }
}
