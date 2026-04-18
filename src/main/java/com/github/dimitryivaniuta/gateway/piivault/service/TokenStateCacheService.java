package com.github.dimitryivaniuta.gateway.piivault.service;

import com.github.dimitryivaniuta.gateway.piivault.config.AppCacheProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Stores non-sensitive token status in Redis to speed up active/deleted checks.
 */
@Service
public class TokenStateCacheService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AppCacheProperties cacheProperties;

    public TokenStateCacheService(ReactiveStringRedisTemplate redisTemplate, AppCacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
    }

    /**
     * Saves a token state with configured TTL.
     *
     * @param token token
     * @param state active or deleted state
     * @return completion signal
     */
    public Mono<Boolean> putState(String token, String state) {
        return redisTemplate.opsForValue().set(key(token), state, cacheProperties.getTokenStateTtl());
    }

    /**
     * Reads a cached token state.
     *
     * @param token token
     * @return cached state or empty
     */
    public Mono<String> getState(String token) {
        return redisTemplate.opsForValue().get(key(token));
    }

    private String key(String token) {
        return "vault:token:state:" + token;
    }
}
