package com.triptrace.domain.auth.auth.exception;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    GOOGLE_AUTH_FAILED("401", "구글 인증에 실패했습니다.");

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
