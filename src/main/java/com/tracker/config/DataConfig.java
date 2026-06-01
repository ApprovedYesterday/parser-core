package com.tracker.config;

import com.tracker.application.port.out.RateLimiterPort;
import com.tracker.infrastructure.ratelimit.RedisRateLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimiterConfig.class)
public class DataConfig {

    @Bean
    RateLimiterPort rateLimiterPort(StringRedisTemplate redisTemplate, RateLimiterConfig config) {
        return new RedisRateLimiter(redisTemplate, config);
    }
}
