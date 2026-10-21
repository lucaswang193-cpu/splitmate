package com.splitmate.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    // Lua script to increment the failure count and set expiration if not already set
    private static final DefaultRedisScript<Long> FAILURE_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if redis.call('TTL', KEYS[1]) < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean isBlocked(String email) {
        String count = redisTemplate.opsForValue().get(key(email));
        return count != null && Long.parseLong(count) >= MAX_FAILURES;
    }

    public long recordFailure(String email) {
        Long count = redisTemplate.execute(
                FAILURE_SCRIPT,
                List.of(key(email)),
                Long.toString(WINDOW.toSeconds()));

        if (count == null) {
            throw new IllegalStateException("Cannot record login failure");
        }
        return count;
    }

    public void clearFailures(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return "auth:login:failures:"
                + email.strip().toLowerCase(Locale.ROOT);
    }
}
