package com.tracker.infrastructure.ratelimit;

import com.tracker.application.port.out.RateLimiterPort;
import com.tracker.config.RateLimiterConfig;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class RedisRateLimiter implements RateLimiterPort {

    private static final String LUA_SCRIPT =
        """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local capacity = tonumber(ARGV[2])
        local refillRate = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])

        local lastTokens = redis.call("GET", key)
        if not lastTokens then
            lastTokens = capacity
        else
            lastTokens = tonumber(lastTokens)
        end

        local lastRefreshed = redis.call("GET", key .. ":time")
        if not lastRefreshed then
            lastRefreshed = 0
        else
            lastRefreshed = tonumber(lastRefreshed)
        end

        local delta = math.max(0, now - lastRefreshed)
        local filled = math.min(capacity, lastTokens + delta * refillRate)
        local allowed = filled >= requested

        local ttl = math.ceil(capacity / math.max(refillRate, 1)) * 2
        if allowed then
            redis.call("SET", key, filled - requested, "EX", ttl)
            redis.call("SET", key .. ":time", now, "EX", ttl)
            return 1
        else
            redis.call("SET", key, filled, "EX", ttl)
            redis.call("SET", key .. ":time", now, "EX", ttl)
            return 0
        end
        """;

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterConfig config;
    private final DefaultRedisScript<Long> script;

    public RedisRateLimiter(StringRedisTemplate redisTemplate, RateLimiterConfig config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptText(LUA_SCRIPT);
        this.script.setResultType(Long.class);
    }

    @Override
    public boolean tryConsume(String key, int tokens) {
        Long result = redisTemplate.execute(
            script,
            List.of("rate_limiter:" + key),
            String.valueOf(System.currentTimeMillis() / 1000),
            String.valueOf(config.capacity()),
            String.valueOf(config.refillRate()),
            String.valueOf(tokens)
        );
        return result != null && result == 1;
    }
}
