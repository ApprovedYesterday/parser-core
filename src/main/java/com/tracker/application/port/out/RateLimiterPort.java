package com.tracker.application.port.out;

public interface RateLimiterPort {
    boolean tryConsume(String key, int tokens);
}
