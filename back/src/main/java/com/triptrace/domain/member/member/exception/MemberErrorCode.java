package com.triptrace.domain.member.member.exception;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.ErrorCode;

public enum MemberErrorCode implements ErrorCode {
    ALREADY_ONBOARDED("409", "이미 온보딩이 완료된 회원입니다.");

    private final String code;
    private final String message;

    MemberErrorCode(String code, String message) {
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
        return Domain.MEMBER;
    }
}
