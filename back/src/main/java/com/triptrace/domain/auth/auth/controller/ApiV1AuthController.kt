package com.triptrace.domain.auth.auth.controller

import com.triptrace.domain.auth.auth.dto.GoogleLoginRequest
import com.triptrace.domain.auth.auth.dto.LoginRequest
import com.triptrace.domain.auth.auth.dto.LoginResponse
import com.triptrace.domain.auth.auth.dto.ReissueResponse
import com.triptrace.domain.auth.auth.dto.SignupRequest
import com.triptrace.domain.auth.auth.dto.SignupResponse
import com.triptrace.domain.auth.auth.service.AuthService
import com.triptrace.global.rsData.RsData
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/v1")
class ApiV1AuthController(
    private val authService: AuthService,
    @param:Value("\${custom.refreshToken.expirationSeconds}") private val refreshTokenExpirationSeconds: Long
) {

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@RequestBody @Valid request: SignupRequest): RsData<SignupResponse> {
        val response = authService.signup(request)

        return RsData("201-1", "회원가입 성공", response)
    }

    @PostMapping("/auth/login")
    fun login(
        @RequestBody @Valid request: LoginRequest,
        response: HttpServletResponse
    ): RsData<LoginResponse> {
        val tokens = authService.login(request)
        addRefreshTokenCookie(response, tokens.refreshToken)

        return RsData("200-1", "로그인 성공", LoginResponse(tokens.accessToken))
    }

    @PostMapping("/auth/oauth/google")
    fun loginWithGoogle(
        @RequestBody @Valid request: GoogleLoginRequest,
        response: HttpServletResponse
    ): RsData<LoginResponse> {
        val result = authService.loginWithGoogle(request.code, request.redirectUri)
        addRefreshTokenCookie(response, result.tokens.refreshToken)

        return RsData(
            "200-1",
            "구글 로그인 성공",
            LoginResponse(result.tokens.accessToken, result.status)
        )
    }

    @PostMapping("/auth/reissue")
    fun reissue(
        @CookieValue(value = "refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): RsData<ReissueResponse> {
        val tokens = authService.reissue(refreshToken)
        addRefreshTokenCookie(response, tokens.refreshToken)

        return RsData("200-1", "토큰 재발급 성공", ReissueResponse(tokens.accessToken))
    }

    @PostMapping("/auth/logout")
    fun logout(
        @CookieValue(value = "refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): RsData<Void> {
        authService.logout(refreshToken)
        expireRefreshTokenCookie(response)

        return RsData("200-1", "로그아웃 성공")
    }

    // RT를 HttpOnly 쿠키로 내려준다. (로그인/재발급 공통)
    private fun addRefreshTokenCookie(response: HttpServletResponse, refreshToken: String?) {
        val cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ofSeconds(refreshTokenExpirationSeconds))
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    // 로그아웃 시 브라우저의 RT 쿠키를 즉시 만료시킨다. (maxAge=0, 동일 path여야 삭제됨)
    private fun expireRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(0)
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
