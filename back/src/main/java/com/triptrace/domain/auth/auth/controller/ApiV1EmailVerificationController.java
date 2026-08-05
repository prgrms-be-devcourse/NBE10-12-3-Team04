package com.triptrace.domain.auth.auth.controller;

import com.triptrace.domain.auth.auth.dto.EmailVerificationCodeRequest;
import com.triptrace.domain.auth.auth.dto.EmailVerifyRequest;
import com.triptrace.domain.auth.auth.service.EmailVerificationService;
import com.triptrace.global.rsData.RsData;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입 전 단계의 이메일 인증 엔드포인트. 로그인 없이 호출한다.
 */
@RestController
@RequestMapping("/api/v1/auth/email")
public class ApiV1EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/verification-code")
    public RsData<Void> issueCode(@RequestBody @Valid EmailVerificationCodeRequest request) {
        emailVerificationService.issueCode(request.email());

        return new RsData<>("200-1", "인증 코드를 발송했습니다.");
    }

    @PostMapping("/verify")
    public RsData<Void> verify(@RequestBody @Valid EmailVerifyRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());

        return new RsData<>("200-1", "이메일 인증이 완료되었습니다.");
    }

    public ApiV1EmailVerificationController(final EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }
}
