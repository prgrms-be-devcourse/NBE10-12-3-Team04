package com.triptrace.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 토큰 발급·검증 로직을 스프링 컨텍스트 없이 직접 확인한다.
 * 만료나 위조처럼 실제 요청으로 만들기 어려운 상황은 키와 만료시간을 바꿔가며 재현한다.
 */
class JwtProviderTest {

    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String OTHER_SECRET = "zyxwvutsrqponmlkjihgfedcba9876543210";
    private static final long ONE_HOUR = 3600L;

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, ONE_HOUR);

    @Test
    @DisplayName("발급한 액세스 토큰은 검증을 통과한다.")
    void validateGeneratedToken() {
        String token = jwtProvider.generateAccessToken(1L, "user@test.com");

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("토큰에서 회원 식별자와 이메일을 그대로 꺼낼 수 있다.")
    void extractClaims() {
        String token = jwtProvider.generateAccessToken(42L, "user@test.com");

        assertThat(jwtProvider.getMemberId(token)).isEqualTo(42L);
        assertThat(jwtProvider.getEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다.")
    void rejectExpiredToken() {
        // 만료시간을 음수로 주면 발급 즉시 만료된 토큰이 나온다.
        JwtProvider expiredProvider = new JwtProvider(SECRET, -ONE_HOUR);
        String token = expiredProvider.generateAccessToken(1L, "user@test.com");

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 검증에 실패한다.")
    void rejectTokenSignedWithOtherKey() {
        JwtProvider attacker = new JwtProvider(OTHER_SECRET, ONE_HOUR);
        String forged = attacker.generateAccessToken(1L, "user@test.com");

        assertThat(jwtProvider.validateToken(forged)).isFalse();
    }

    @Test
    @DisplayName("서명 부분이 변조된 토큰은 검증에 실패한다.")
    void rejectTamperedToken() {
        String token = jwtProvider.generateAccessToken(1L, "user@test.com");
        String[] parts = token.split("\\.");

        // 서명의 마지막 글자는 데이터 4비트 + 미사용 2비트라, 바꿔도 디코딩 결과가 그대로일 수 있다.
        // 6비트를 온전히 담는 첫 글자를 바꿔야 서명 바이트가 확실히 달라진다.
        char head = parts[2].charAt(0);
        String tamperedSignature = (head == 'a' ? 'b' : 'a') + parts[2].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertThat(jwtProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("JWT 형식이 아닌 문자열은 검증에 실패한다.")
    void rejectMalformedToken() {
        assertThat(jwtProvider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("null이나 빈 문자열도 예외 없이 false를 반환한다.")
    void rejectNullOrBlankToken() {
        // 헤더가 비어 있는 요청에서도 필터가 터지지 않아야 하므로 예외 대신 false로 처리한다.
        assertThat(jwtProvider.validateToken(null)).isFalse();
        assertThat(jwtProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰은 호출할 때마다 다른 값이 나온다.")
    void generateUniqueRefreshToken() {
        String first = jwtProvider.generateRefreshToken();
        String second = jwtProvider.generateRefreshToken();

        assertThat(first).isNotEqualTo(second);
    }
}
