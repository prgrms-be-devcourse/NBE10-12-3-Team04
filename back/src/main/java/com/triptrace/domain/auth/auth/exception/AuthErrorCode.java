package com.triptrace.domain.auth.auth.exception;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    GOOGLE_AUTH_FAILED("401", "구글 인증에 실패했습니다."),
    EMAIL_NOT_VERIFIED("403", "구글에서 인증되지 않은 이메일입니다."),
    ALREADY_REGISTERED("409", "이미 가입된 이메일입니다."),
    USERNAME_GENERATION_FAILED("500", "임시 닉네임 생성에 실패했습니다.");

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
