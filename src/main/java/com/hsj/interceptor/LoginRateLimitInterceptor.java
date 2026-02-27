package com.hsj.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsj.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 로그인 브루트포스 방어 인터셉터.
 * IP당 60초 동안 최대 {@value #MAX_ATTEMPTS}회 시도를 허용한다.
 * 초과 시 HTTP 429 Too Many Requests 응답을 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX   = "RATE_LIMIT:LOGIN:";
    private static final int    MAX_ATTEMPTS = 5;
    private static final long   WINDOW_SEC   = 60L;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String clientIp = resolveClientIp(request);
        String key = KEY_PREFIX + clientIp;

        Long count = redisTemplate.opsForValue().increment(key);

        // 첫 요청이면 TTL 설정
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW_SEC, TimeUnit.SECONDS);
        }

        if (count != null && count > MAX_ATTEMPTS) {
            log.warn("로그인 Rate Limit 초과: ip={}, count={}", clientIp, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Void> body = ApiResponse.error(
                    "AUTH006", "요청이 너무 많습니다. " + WINDOW_SEC + "초 후에 다시 시도해주세요.");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }

        return true;
    }

    /**
     * X-Forwarded-For 헤더(프록시/로드밸런서 환경)를 우선 확인하고,
     * 없으면 RemoteAddr을 사용한다.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
