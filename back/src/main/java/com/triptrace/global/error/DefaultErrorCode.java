package com.triptrace.global.error;

import com.triptrace.global.app.Domain;


public enum DefaultErrorCode implements ErrorCode {
    //Default error code & Domain number & message
    BAD_REQUEST(        "400",      "올바르지 않은 요청입니다."),
    UNAUTHORIZED(       "401",      "인증되지 않은 요청입니다."),
    FORBIDDEN(          "403",      "권한이 없습니다."),
    NOT_FOUND(          "404",      "해당 데이터가 존재하지 않습니다."),
    CONFLICT(           "409",      "중복된 값입니다."),
    PAYLOAD_TOO_LARGE(  "413",      "파일 크기가 용량을 초과합니다.");

    private final String code;
    private final String message;

    DefaultErrorCode(String code, Domain tag, String message) {
        this.code = String.format("%s-%s",  code, tag.getCode());
        this.message = message;
    }

    DefaultErrorCode(String code, String message) {
        this(code, Domain.COMMON, message);
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
