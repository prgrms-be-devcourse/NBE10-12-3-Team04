package com.triptrace.domain.auth.auth.service

import com.triptrace.domain.auth.auth.dto.LoginRequest
import com.triptrace.domain.auth.auth.dto.OAuthLoginResult
import com.triptrace.domain.auth.auth.dto.SignupRequest
import com.triptrace.domain.auth.auth.dto.SignupResponse
import com.triptrace.domain.auth.auth.dto.TokenPair
import com.triptrace.domain.auth.auth.entity.RefreshToken
import com.triptrace.domain.auth.auth.exception.AlreadyRegisteredException
import com.triptrace.domain.auth.auth.exception.AuthErrorCode
import com.triptrace.domain.auth.auth.oauth.GoogleOAuthClient
import com.triptrace.domain.auth.auth.oauth.GoogleUserInfo
import com.triptrace.domain.auth.auth.oauth.UsernameGenerator
import com.triptrace.domain.auth.auth.repository.RefreshTokenRepository
import com.triptrace.domain.member.member.entity.LoginType
import com.triptrace.domain.member.member.entity.Member
import com.triptrace.domain.member.member.repository.MemberRepository
import com.triptrace.domain.member.member.service.MemberService
import com.triptrace.global.exception.ServiceException
import com.triptrace.global.security.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 인증 흐름을 조립하는 서비스. (비밀번호 해시/검증, 토큰 발급/재발급 등)
 * 회원 데이터의 중복검사·저장·조회는 MemberService에 위임한다.
 */
@Service
class AuthService(
    private val memberService: MemberService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenService: RefreshTokenService,
    private val emailVerificationService: EmailVerificationService,
    private val googleOAuthClient: GoogleOAuthClient,
    private val memberRepository: MemberRepository,
    private val usernameGenerator: UsernameGenerator,
    @param:Value("\${custom.refreshToken.expirationSeconds}") private val refreshTokenExpirationSeconds: Long
) {

    // 회원가입: 이메일 인증을 마쳤는지 먼저 확인하고, 비밀번호를 해시한 뒤 회원 저장을 MemberService에 위임한다.
    fun signup(request: SignupRequest): SignupResponse {
        // 인증 완료 여부와 인증 후 30분 유효기간까지 EmailVerificationService가 함께 판단한다.
        if (!emailVerificationService.isEmailVerified(request.email)) {
            throw ServiceException(AuthErrorCode.SIGNUP_EMAIL_NOT_VERIFIED)
        }

        val passwordHash = passwordEncoder.encode(request.password)

        val member = memberService.signup(
            request.email,
            request.username,
            passwordHash,
            request.profileImageUrl
        )

        return SignupResponse(member)
    }

    // 로그인: 이메일 조회 → 비밀번호 검증 → AT 발급 + RT 발급·저장. RT 문자열은 컨트롤러가 쿠키로 처리한다.
    @Transactional
    fun login(request: LoginRequest): TokenPair {
        val member = memberService.findByEmail(request.email)
        if (!passwordEncoder.matches(request.password, member.passwordHash)) {
            throw ServiceException("401-1", "이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        return issueTokens(member)
    }

    // 구글 로그인: 인가 코드 → 사용자 정보 → (기존 회원 조회 | 신규 가입) → LOCAL 로그인과 같은 토큰 발급.
    @Transactional
    fun loginWithGoogle(code: String?, redirectUri: String?): OAuthLoginResult {
        val googleAccessToken = googleOAuthClient.exchangeToken(code, redirectUri)
        val userInfo = GoogleUserInfo.from(googleOAuthClient.fetchUserInfo(googleAccessToken))

        // 구글이 소유를 확인하지 않은 이메일은 같은 주소를 선점당할 수 있어 가입/로그인을 막는다.
        if (!userInfo.isEmailVerified) {
            throw ServiceException(AuthErrorCode.EMAIL_NOT_VERIFIED)
        }

        val member = memberRepository
            .findByProviderAndProviderId(LoginType.GOOGLE, userInfo.providerId)
            .orElseGet { registerGoogleMember(userInfo) }

        return OAuthLoginResult(issueTokens(member), member.status)
    }

    // 구글로 처음 들어온 사용자를 가입시킨다. 같은 이메일이 다른 경로로 이미 있으면 자동 연결하지 않고 막는다.
    private fun registerGoogleMember(userInfo: GoogleUserInfo): Member {
        memberRepository.findByEmail(userInfo.email)
            .ifPresent { existing -> throw AlreadyRegisteredException(existing.provider) }

        return memberRepository.save(
            Member.ofOAuth(
                userInfo.email,
                LoginType.GOOGLE,
                userInfo.providerId,
                usernameGenerator.generate(userInfo.email),
                userInfo.profileImageUrl
            )
        )
    }

    // 재발급(Rotation): RT 검증 → 탈취/만료 확인 → 기존 RT 폐기 → 새 AT/RT 발급.
    @Transactional
    fun reissue(refreshToken: String): TokenPair {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow { ServiceException("401-1", "유효하지 않은 리프레시 토큰입니다.") }

        // 탈취 감지: 이미 폐기된 RT가 다시 쓰였다면 해당 회원의 모든 RT를 무효화한다.
        // 아래 예외로 이 재발급 트랜잭션은 롤백되므로, 무효화는 별도 트랜잭션(REQUIRES_NEW)에서 커밋한다.
        if (storedToken.isRevoked) {
            refreshTokenService.revokeAllByMember(storedToken.member)

            throw ServiceException("401-1", "토큰이 탈취되었을 가능성이 있습니다. 재로그인 해주세요.")
        }

        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw ServiceException("401-1", "만료된 리프레시 토큰입니다.")
        }

        // 기존 RT는 삭제하지 않고 폐기 표시만 한 뒤, 새 토큰을 발급한다.
        storedToken.revoke()

        return issueTokens(storedToken.member)
    }

    // 로그아웃: 전달받은 RT를 폐기 표시한다. (같은 트랜잭션의 dirty checking으로 UPDATE 반영)
    @Transactional
    fun logout(refreshToken: String) {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow { ServiceException("401-1", "유효하지 않은 리프레시 토큰입니다.") }

        storedToken.revoke()
    }

    // AT 발급 + 새 RT 발급·저장 후 한 쌍으로 반환. (로그인/재발급 공통)
    private fun issueTokens(member: Member): TokenPair {
        val accessToken = jwtProvider.generateAccessToken(member.id, member.email)
        val refreshToken = jwtProvider.generateRefreshToken()

        val expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds)
        refreshTokenRepository.save(RefreshToken(member, refreshToken, expiresAt))

        return TokenPair(accessToken, refreshToken)
    }
}
