package com.triptrace.domain.auth.auth.controller;

import com.triptrace.domain.auth.auth.oauth.GoogleOAuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AuthGoogleLoginControllerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GoogleOAuthClient googleOAuthClient;

    @BeforeEach
    void setUpTokenExchange() {
        given(googleOAuthClient.exchangeToken(anyString(), anyString())).willReturn("google-access-token");
    }

    @Test
    @DisplayName("구글 로그인 API - 성공 시 200 + AT/status 응답 + RT 쿠키")
    void loginWithGoogle() throws Exception {
        givenGoogleUser("controller-sub-1", "controller@gmail.com");

        mvc.perform(post("/api/v1/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "auth-code",
                      "redirectUri": "http://localhost:3000/oauth/callback/google"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.status").value("PENDING_PROFILE"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    @DisplayName("구글 로그인 API - code가 비어 있으면 400")
    void loginWithGoogleBlankCode() throws Exception {
        mvc.perform(post("/api/v1/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "",
                      "redirectUri": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400"));
    }

    @Test
    @DisplayName("구글 로그인 API - 구글 통신에 실패하면 401")
    void loginWithGoogleFailure() throws Exception {
        given(googleOAuthClient.exchangeToken(anyString(), anyString()))
            .willThrow(new com.triptrace.global.exception.ServiceException(
                com.triptrace.domain.auth.auth.exception.AuthErrorCode.GOOGLE_AUTH_FAILED));

        mvc.perform(post("/api/v1/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "invalid-code",
                      "redirectUri": "http://localhost:3000/oauth/callback/google"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("401-02"));
    }

    private void givenGoogleUser(String sub, String email) {
        ObjectNode attributes = objectMapper.createObjectNode();
        attributes.put("sub", sub);
        attributes.put("email", email);
        attributes.put("email_verified", true);
        attributes.put("name", "테스터");
        attributes.put("picture", "https://example.com/p.png");

        given(googleOAuthClient.fetchUserInfo(anyString())).willReturn(attributes);
    }
}
