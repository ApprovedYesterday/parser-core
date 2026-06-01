package com.tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ozon.rate-limiter")
public record RateLimiterConfig(int capacity, int refillRate) {}
