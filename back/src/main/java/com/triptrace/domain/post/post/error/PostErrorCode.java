package com.triptrace.domain.post.post.error;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.DefaultErrorCode;
import com.triptrace.global.error.ErrorCode;

public enum PostErrorCode implements ErrorCode {
    TRIP_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "여행기를 찾을 수 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "게시물을 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN.getCode(), "여행기에 대한 권한이 없습니다.");

    private final String code;
    private final String message;

    PostErrorCode(String code, String message) {
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
        return Domain.POST;
    }
}
