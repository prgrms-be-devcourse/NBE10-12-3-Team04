package com.triptrace.global.errorCode;

import com.triptrace.global.app.DomainNumber;


public enum GlobalErrorCode implements ErrorCode {
    //Default error code & Domain number & message
    INVALID(            "400",    DomainNumber.GLOBAL,  "올바르지 않은 요청입니다."),
    UNAUTHORIZED(       "401",    DomainNumber.GLOBAL,  "인증되지 않은 요청입니다."),
    FORBIDDEN(          "403",    DomainNumber.GLOBAL,  "권한이 없습니다."),
    NOT_FOUND(          "404",    DomainNumber.GLOBAL,  "해당 데이터가 존재하지 않습니다."),
    DUPLICATE(          "409",    DomainNumber.GLOBAL,  "중복된 값입니다."),
    PAYLOAD_TOO_LARGE(  "413",    DomainNumber.GLOBAL,  "파일 크기가 용량을 초과합니다.");

    private final String code;
    private final DomainNumber tag;
    private final String message;

    GlobalErrorCode(String code, DomainNumber tag, String message){
        this.code = code;
        this.tag = tag;
        this.message = message;
    }
    @Override
    public String getCode() {
        return "%s-%s".formatted(code, tag.getCode());
    }

    @Override
    public String getMessage() {
        return message;
    }
}
