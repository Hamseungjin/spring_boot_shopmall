package com.hsj.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    // 테스트용 256-bit Base64 인코딩 시크릿 키
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW11c3QtYmUtMjU2LWJpdHM=";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "secretString", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 1800000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L);
        jwtTokenProvider.init(); // @PostConstruct 직접 호출
    }

    // ─────────────────────────── 토큰 생성 ───────────────────────────

    @Test
    @DisplayName("createAccessToken: 정상적으로 토큰이 발급된다")
    void createAccessToken_정상발급() {
        String token = jwtTokenProvider.createAccessToken(1L, "test@test.com", "ROLE_CUSTOMER");

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("createRefreshToken: 토큰을 생성하고 Redis에 저장한다")
    void createRefreshToken_생성_및_레디스저장() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        String token = jwtTokenProvider.createRefreshToken(1L);

        assertThat(token).isNotNull().isNotEmpty();
        verify(valueOps).set(eq("RT:1"), eq(token), eq(604800000L), eq(TimeUnit.MILLISECONDS));
    }

    // ─────────────────────────── 토큰 검증 ───────────────────────────

    @Test
    @DisplayName("validateToken: 유효한 토큰이면 true를 반환한다")
    void validateToken_유효한토큰_true() {
        String token = jwtTokenProvider.createAccessToken(1L, "test@test.com", "ROLE_CUSTOMER");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: 변조된 토큰이면 false를 반환한다")
    void validateToken_변조된토큰_false() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.string")).isFalse();
    }

    @Test
    @DisplayName("validateToken: 빈 문자열이면 false를 반환한다")
    void validateToken_빈문자열_false() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken: 만료된 토큰이면 false를 반환한다")
    void validateToken_만료된토큰_false() {
        // 만료 시간 -1ms 로 이미 만료된 토큰 생성
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", -1L);
        String expiredToken = jwtTokenProvider.createAccessToken(1L, "test@test.com", "ROLE_CUSTOMER");

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    // ─────────────────────────── 클레임 추출 ───────────────────────────

    @Test
    @DisplayName("getMemberIdFromToken: 토큰에서 memberId를 정확히 추출한다")
    void getMemberIdFromToken_정상추출() {
        String token = jwtTokenProvider.createAccessToken(42L, "test@test.com", "ROLE_CUSTOMER");
        Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

        assertThat(memberId).isEqualTo(42L);
    }

    @Test
    @DisplayName("isTokenExpired: 만료된 토큰이면 true를 반환한다")
    void isTokenExpired_만료된토큰_true() {
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", -1L);
        String expiredToken = jwtTokenProvider.createAccessToken(1L, "test@test.com", "ROLE_CUSTOMER");

        assertThat(jwtTokenProvider.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("isTokenExpired: 유효한 토큰이면 false를 반환한다")
    void isTokenExpired_유효한토큰_false() {
        String token = jwtTokenProvider.createAccessToken(1L, "test@test.com", "ROLE_CUSTOMER");
        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
    }

    // ─────────────────────────── 블랙리스트 ───────────────────────────

    @Test
    @DisplayName("addToBlacklist: 남은 시간이 양수이면 Redis에 저장한다")
    void addToBlacklist_양수잔여시간_저장() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        String token = jwtTokenProvider.createAccessToken(1L, "test@test.com", "ROLE_CUSTOMER");

        jwtTokenProvider.addToBlacklist(token, 5000L);

        verify(valueOps).set(startsWith("BL:"), eq("logout"), eq(5000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("addToBlacklist: 남은 시간이 0 이하이면 Redis에 저장하지 않는다")
    void addToBlacklist_만료된시간_저장안함() {
        String token = "some.expired.token";

        jwtTokenProvider.addToBlacklist(token, 0L);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("isBlacklisted: Redis에 키가 있으면 true를 반환한다")
    void isBlacklisted_키존재_true() {
        String token = "some.token.value";
        when(redisTemplate.hasKey("BL:" + token)).thenReturn(true);

        assertThat(jwtTokenProvider.isBlacklisted(token)).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted: Redis에 키가 없으면 false를 반환한다")
    void isBlacklisted_키없음_false() {
        String token = "some.token.value";
        when(redisTemplate.hasKey("BL:" + token)).thenReturn(false);

        assertThat(jwtTokenProvider.isBlacklisted(token)).isFalse();
    }

    // ─────────────────────────── 리프레시 토큰 관리 ───────────────────────────

    @Test
    @DisplayName("deleteRefreshToken: Redis에서 리프레시 토큰 키를 삭제한다")
    void deleteRefreshToken_키삭제() {
        jwtTokenProvider.deleteRefreshToken(1L);

        verify(redisTemplate).delete("RT:1");
    }

    @Test
    @DisplayName("validateRefreshToken: 저장된 토큰과 일치하면 true를 반환한다")
    void validateRefreshToken_일치_true() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("RT:1")).thenReturn("stored-token");

        assertThat(jwtTokenProvider.validateRefreshToken(1L, "stored-token")).isTrue();
    }

    @Test
    @DisplayName("validateRefreshToken: 저장된 토큰과 불일치하면 false를 반환한다")
    void validateRefreshToken_불일치_false() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("RT:1")).thenReturn("stored-token");

        assertThat(jwtTokenProvider.validateRefreshToken(1L, "different-token")).isFalse();
    }
}
