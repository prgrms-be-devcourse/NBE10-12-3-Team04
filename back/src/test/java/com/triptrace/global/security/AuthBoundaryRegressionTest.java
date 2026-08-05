package com.triptrace.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 값이 비어 있는 요청이 어디서 걸리는지 고정한다.
 *
 * 코틀린 전환 과정에서 파라미터를 non-null로 선언하면 스프링이 넣어주는 null이
 * 401이 아니라 NPE(500)로 바뀐다. 이 경계가 다시 깨지지 않도록 HTTP 레벨에서 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthBoundaryRegressionTest {

    @Autowired
    private MockMvc mvc;

    // 아래 두 건이 403인 것은 SecurityConfig에 AuthenticationEntryPoint를 두지 않아
    // 스프링 시큐리티 기본값이 적용되기 때문이다. (마이그레이션 이전부터 같은 동작)
    // 여기서 확인하려는 것은 코드가 401이냐 403이냐가 아니라, 컨트롤러에 닿기 전에 막혀 500이 되지 않는다는 점이다.
    @Test
    @DisplayName("토큰 없이 내 정보를 조회하면 컨트롤러에 닿기 전에 막힌다.")
    void getMeWithoutToken() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("형식이 잘못된 토큰으로 내 정보를 조회해도 마찬가지로 막힌다.")
    void getMeWithMalformedToken() throws Exception {
        // JwtFilter는 무효한 토큰을 그냥 통과시키고, 접근 차단은 SecurityConfig의 규칙이 맡는다.
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-jwt"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RT 쿠키 없이 재발급을 호출하면 500이 아니라 401이다.")
    void reissueWithoutCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/reissue"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("RT 쿠키 없이 로그아웃을 호출하면 500이 아니라 401이다.")
    void logoutWithoutCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("401-1"));
    }
}
