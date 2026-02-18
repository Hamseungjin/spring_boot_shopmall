package com.hsj.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeStatsService {

    private static final String ONLINE_USERS_KEY = "STATS:ONLINE_USERS";
    private static final String PRODUCT_VIEW_PREFIX = "STATS:PRODUCT_VIEW:";
    private static final String DAILY_VISITORS_PREFIX = "STATS:DAILY_VISITORS:";
    private static final long SESSION_TTL_SECONDS = 300;

    private final RedisTemplate<String, String> redisTemplate;

    public void recordUserOnline(String sessionId) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, sessionId);
        redisTemplate.expire(ONLINE_USERS_KEY, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void removeUserOnline(String sessionId) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, sessionId);
    }

    public long getOnlineUserCount() {
        Long size = redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
        return size != null ? size : 0;
    }

    public void incrementProductView(Long productId) {
        String key = PRODUCT_VIEW_PREFIX + productId;
        redisTemplate.opsForValue().increment(key);
    }

    public long getProductViewCount(Long productId) {
        String value = redisTemplate.opsForValue().get(PRODUCT_VIEW_PREFIX + productId);
        return value != null ? Long.parseLong(value) : 0;
    }

    public void recordDailyVisitor(String date, String sessionId) {
        String key = DAILY_VISITORS_PREFIX + date;
        redisTemplate.opsForHyperLogLog().add(key, sessionId);
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    public long getDailyVisitorCount(String date) {
        Long count = redisTemplate.opsForHyperLogLog().size(DAILY_VISITORS_PREFIX + date);
        return count != null ? count : 0;
    }
}
