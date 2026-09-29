package com.dts.scheduler.redis;

import com.dts.scheduler.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${dts.rate-limit.requests-per-minute:120}")
    private int defaultRequestsPerMinute;

    public void checkRateLimit(String keyIdentifier) {
        checkRateLimit(keyIdentifier, defaultRequestsPerMinute, 60);
    }

    public void checkRateLimit(String keyIdentifier, int maxRequests, int windowSeconds) {
        String redisKey = "rate_limit:" + keyIdentifier;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }

            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for key={}, current count={}, max allowed={}", keyIdentifier, count, maxRequests);
                throw new RateLimitExceededException("Rate limit exceeded. Maximum " + maxRequests + " requests allowed per minute.");
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis rate limiter check error (failing open): {}", e.getMessage());
        }
    }
}
