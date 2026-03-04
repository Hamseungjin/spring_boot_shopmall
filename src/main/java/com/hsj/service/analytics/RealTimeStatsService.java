package com.hsj.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
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

    /**
     * 세션을 Sorted Set에 현재 타임스탬프(score)로 등록한다.
     * score 기반으로 개별 세션의 활성 시각을 추적하므로 세션마다 독립적으로 만료된다.
     */
    public void recordUserOnline(String sessionId) {
        double now = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(ONLINE_USERS_KEY, sessionId, now);
    }

    /**
     * 세션을 Sorted Set에서 즉시 제거한다 (로그아웃/연결 종료 시).
     */
    public void removeUserOnline(String sessionId) {
        redisTemplate.opsForZSet().remove(ONLINE_USERS_KEY, sessionId);
    }

    /**
     * TTL_SECONDS 이내에 활동한 세션 수를 반환한다.
     * 조회 전 오래된 세션을 정리하여 정확한 카운트를 보장한다.
     */
    public long getOnlineUserCount() {
        long now = System.currentTimeMillis();
        long cutoff = now - SESSION_TTL_SECONDS * 1000;
        // 만료된 세션(활동 시각이 cutoff 이전) 제거
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_USERS_KEY, 0, cutoff);
        Long size = redisTemplate.opsForZSet().zCard(ONLINE_USERS_KEY);
        return size != null ? size : 0;
    }

    /** 상품 조회수 증가 — 실패해도 상품 조회에 영향 없으므로 비동기 처리 */
    @Async("statsExecutor")
    public void incrementProductView(Long productId) {
        String key = PRODUCT_VIEW_PREFIX + productId;
        redisTemplate.opsForValue().increment(key);
    }

    public long getProductViewCount(Long productId) {
        String value = redisTemplate.opsForValue().get(PRODUCT_VIEW_PREFIX + productId);
        return value != null ? Long.parseLong(value) : 0;
    }

    /** 일일 방문자 기록 — 통계 목적이므로 시차 허용, 비동기 처리 */
    @Async("statsExecutor")
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
