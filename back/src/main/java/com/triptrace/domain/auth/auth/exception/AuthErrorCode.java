package com.triptrace.domain.auth.auth.exception;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    // 구글 OAuth
    GOOGLE_AUTH_FAILED("401", "구글 인증에 실패했습니다."),
    EMAIL_NOT_VERIFIED("403", "구글에서 인증되지 않은 이메일입니다."),
    ALREADY_REGISTERED("409", "이미 가입된 이메일입니다."),
    USERNAME_GENERATION_FAILED("500", "임시 닉네임 생성에 실패했습니다."),

    // 이메일 인증 회원가입
    VERIFICATION_CODE_NOT_FOUND("404", "발급된 인증 코드가 없습니다. 인증 코드를 먼저 요청해 주세요."),
    VERIFICATION_CODE_COOLDOWN("429", "인증 코드는 60초 후에 다시 요청할 수 있습니다."),
    VERIFICATION_CODE_MISMATCH("400", "인증 코드가 일치하지 않습니다."),
    VERIFICATION_CODE_EXPIRED("400", "만료된 인증 코드입니다. 인증 코드를 다시 요청해 주세요."),
    VERIFICATION_ATTEMPT_EXCEEDED("429", "인증 시도 횟수를 초과했습니다. 인증 코드를 다시 요청해 주세요."),
    VERIFICATION_ALREADY_VERIFIED("409", "이미 인증이 완료된 이메일입니다."),
    SIGNUP_EMAIL_NOT_VERIFIED("403", "이메일 인증이 필요합니다."),
    EMAIL_SEND_FAILED("500", "인증 메일 발송에 실패했습니다.");

    private final String code;
    private final String message;

    AuthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Domain getDomain() {
        return Domain.AUTH;
    }
}
