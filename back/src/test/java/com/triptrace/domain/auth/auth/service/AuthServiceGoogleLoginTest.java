package com.triptrace.domain.auth.auth.service;

import com.triptrace.domain.auth.auth.dto.OAuthLoginResult;
import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.entity.EmailVerification;
import com.triptrace.domain.auth.auth.entity.RefreshToken;
import com.triptrace.domain.auth.auth.exception.AlreadyRegisteredException;
import com.triptrace.domain.auth.auth.oauth.GoogleOAuthClient;
import com.triptrace.domain.auth.auth.repository.EmailVerificationRepository;
import com.triptrace.domain.auth.auth.repository.RefreshTokenRepository;
import com.triptrace.domain.member.member.entity.LoginType;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 구글 로그인은 외부 HTTP 호출에 의존하므로 GoogleOAuthClient만 대역으로 바꾸고
 * 그 뒤의 회원 판별·가입·토큰 발급은 실제 흐름 그대로 검증한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthServiceGoogleLoginTest {

    private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback/google";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockitoBean
    private GoogleOAuthClient googleOAuthClient;

    @BeforeEach
    void setUpTokenExchange() {
        given(googleOAuthClient.exchangeToken(anyString(), anyString())).willReturn("google-access-token");
    }

    @Test
    @DisplayName("구글 신규 로그인 시 회원이 생성되고 AT/RT가 발급된다.")
    void loginWithGoogleCreatesMemberAndIssuesTokens() {
        givenGoogleUser("google-sub-1", "newbie@gmail.com", true);

        OAuthLoginResult result = authService.loginWithGoogle("code", REDIRECT_URI);

        assertThat(result.tokens().accessToken()).isNotBlank();
        assertThat(result.tokens().refreshToken()).isNotBlank();
        // 온보딩 전이므로 PENDING_PROFILE 상태로 만들어진다.
        assertThat(result.status()).isEqualTo(MemberStatus.PENDING_PROFILE);

        Member member = memberRepository.findByProviderAndProviderId(LoginType.GOOGLE, "google-sub-1").orElseThrow();
        assertThat(member.getEmail()).isEqualTo("newbie@gmail.com");
        assertThat(member.getPasswordHash()).isNull();
        assertThat(member.getUsername()).isNotBlank();

        // RT는 DB에 저장되고 유효한 상태여야 한다. (LOCAL 로그인과 동일한 발급 경로)
        RefreshToken savedToken = refreshTokenRepository.findByToken(result.tokens().refreshToken()).orElseThrow();
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("같은 구글 계정으로 다시 로그인해도 회원이 중복 생성되지 않는다.")
    void loginWithGoogleTwiceReusesMember() {
        givenGoogleUser("google-sub-2", "repeat@gmail.com", true);

        OAuthLoginResult first = authService.loginWithGoogle("code-1", REDIRECT_URI);
        long countAfterFirst = memberRepository.count();

        OAuthLoginResult second = authService.loginWithGoogle("code-2", REDIRECT_URI);

        assertThat(memberRepository.count()).isEqualTo(countAfterFirst);
        assertThat(memberRepository.findAll())
            .filteredOn(member -> "google-sub-2".equals(member.getProviderId()))
            .hasSize(1);
        // 매 로그인마다 새 토큰이 나가지만 회원은 그대로다.
        assertThat(second.tokens().refreshToken()).isNotEqualTo(first.tokens().refreshToken());
        assertThat(second.status()).isEqualTo(first.status());
    }

    @Test
    @DisplayName("온보딩을 마친 구글 회원이 재로그인하면 ACTIVE 상태로 응답한다.")
    void loginWithGoogleAfterOnboardingReturnsActive() {
        givenGoogleUser("google-sub-3", "onboarded@gmail.com", true);
        authService.loginWithGoogle("code-1", REDIRECT_URI);

        Member member = memberRepository.findByProviderAndProviderId(LoginType.GOOGLE, "google-sub-3").orElseThrow();
        member.completeProfile("확정닉네임");

        OAuthLoginResult result = authService.loginWithGoogle("code-2", REDIRECT_URI);

        assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("이미 LOCAL로 가입된 이메일이면 구글 가입을 막고 기존 가입 경로를 알려준다.")
    void loginWithGoogleRejectsEmailTakenByLocalMember() {
        markEmailVerified("shared@test.com");

        authService.signup(new SignupRequest("shared@test.com", "localuser", "password1234", null));
        givenGoogleUser("google-sub-4", "shared@test.com", true);

        assertThatThrownBy(() -> authService.loginWithGoogle("code", REDIRECT_URI))
            .isInstanceOf(AlreadyRegisteredException.class)
            .hasMessageContaining("이미 이메일로 가입된 이메일입니다.")
            .extracting(exception -> ((AlreadyRegisteredException) exception).getProvider())
            .isEqualTo(LoginType.LOCAL);

        // 기존 LOCAL 회원은 그대로 남고 구글 회원이 새로 생기지 않는다.
        assertThat(memberRepository.findByProviderAndProviderId(LoginType.GOOGLE, "google-sub-4")).isEmpty();
        assertThat(memberRepository.findByEmail("shared@test.com").orElseThrow().getProvider())
            .isEqualTo(LoginType.LOCAL);
    }

    @Test
    @DisplayName("구글이 인증하지 않은 이메일이면 로그인할 수 없다.")
    void loginWithGoogleRejectsUnverifiedEmail() {
        givenGoogleUser("google-sub-5", "unverified@gmail.com", false);

        assertThatThrownBy(() -> authService.loginWithGoogle("code", REDIRECT_URI))
            .isInstanceOf(ServiceException.class)
            .hasMessage("403-02 : 구글에서 인증되지 않은 이메일입니다.");

        assertThat(memberRepository.findByEmail("unverified@gmail.com")).isEmpty();
    }

    @Test
    @DisplayName("구글 사용자 정보에 sub가 없으면 로그인할 수 없다.")
    void loginWithGoogleRejectsMissingSub() {
        ObjectNode attributes = objectMapper.createObjectNode();
        attributes.put("email", "nosub@gmail.com");
        attributes.put("email_verified", true);
        given(googleOAuthClient.fetchUserInfo(anyString())).willReturn(attributes);

        assertThatThrownBy(() -> authService.loginWithGoogle("code", REDIRECT_URI))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("구글 사용자 정보 응답에 sub가 없습니다.");
    }

    // LOCAL 회원가입은 이메일 인증을 마친 주소만 통과하므로, 인증 완료 레코드를 미리 심어둔다.
    private void markEmailVerified(String email) {
        EmailVerification verification =
            EmailVerification.issue(email, "123456", LocalDateTime.now().plusMinutes(5));
        verification.verify();

        emailVerificationRepository.save(verification);
    }

    private void givenGoogleUser(String sub, String email, boolean emailVerified) {
        ObjectNode attributes = objectMapper.createObjectNode();
        attributes.put("sub", sub);
        attributes.put("email", email);
        attributes.put("email_verified", emailVerified);
        attributes.put("name", "테스터");
        attributes.put("picture", "https://example.com/profile.png");

        given(googleOAuthClient.fetchUserInfo(anyString())).willReturn(attributes);
    }
}
