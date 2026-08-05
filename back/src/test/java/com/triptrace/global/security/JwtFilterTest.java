package com.triptrace.global.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 요청 헤더의 토큰을 어떤 경우에 인증으로 인정하는지 확인한다.
 * 토큰이 없거나 무효여도 필터는 요청을 막지 않고 통과시켜야 한다. (접근 제어는 SecurityConfig의 몫)
 */
class JwtFilterTest {

    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final long ONE_HOUR = 3600L;

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, ONE_HOUR);
    private final JwtFilter jwtFilter = new JwtFilter(jwtProvider);

    @AfterEach
    void clearContext() {
        // SecurityContext는 스레드에 남으므로 테스트끼리 인증 상태가 새지 않도록 매번 비운다.
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰이 오면 회원 식별자로 인증 정보를 등록한다.")
    void authenticateWithValidToken() throws ServletException, IOException {
        String token = jwtProvider.generateAccessToken(7L, "user@test.com");
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(requestWithHeader("Bearer " + token), new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 없이 통과시킨다.")
    void passThroughWithoutHeader() throws ServletException, IOException {
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 헤더는 토큰으로 보지 않는다.")
    void ignoreNonBearerHeader() throws ServletException, IOException {
        String token = jwtProvider.generateAccessToken(7L, "user@test.com");

        jwtFilter.doFilter(requestWithHeader(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("무효한 토큰이면 인증을 등록하지 않되 요청은 계속 진행한다.")
    void passThroughWithInvalidToken() throws ServletException, IOException {
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(requestWithHeader("Bearer not-a-jwt"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // 필터가 요청을 삼키면 이후 체인이 돌지 않아 permitAll 경로까지 막힌다.
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("만료된 토큰으로는 인증되지 않는다.")
    void rejectExpiredToken() throws ServletException, IOException {
        JwtProvider expiredProvider = new JwtProvider(SECRET, -ONE_HOUR);
        String expired = expiredProvider.generateAccessToken(7L, "user@test.com");

        jwtFilter.doFilter(requestWithHeader("Bearer " + expired), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requestWithHeader(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);

        return request;
    }
}
