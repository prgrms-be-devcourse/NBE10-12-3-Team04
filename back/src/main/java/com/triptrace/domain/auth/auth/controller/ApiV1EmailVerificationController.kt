package com.triptrace.domain.auth.auth.controller

import com.triptrace.domain.auth.auth.dto.EmailVerificationCodeRequest
import com.triptrace.domain.auth.auth.dto.EmailVerifyRequest
import com.triptrace.domain.auth.auth.service.EmailVerificationService
import com.triptrace.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 회원가입 전 단계의 이메일 인증 엔드포인트. 로그인 없이 호출한다.
 */
@RestController
@RequestMapping("/api/v1/auth/email")
class ApiV1EmailVerificationController(
    private val emailVerificationService: EmailVerificationService
) {

    @PostMapping("/verification-code")
    fun issueCode(@RequestBody @Valid request: EmailVerificationCodeRequest): RsData<Void> {
        emailVerificationService.issueCode(request.email)

        return RsData("200-1", "인증 코드를 발송했습니다.")
    }

    @PostMapping("/verify")
    fun verify(@RequestBody @Valid request: EmailVerifyRequest): RsData<Void> {
        emailVerificationService.verifyCode(request.email, request.code)

        return RsData("200-1", "이메일 인증이 완료되었습니다.")
    }
}
