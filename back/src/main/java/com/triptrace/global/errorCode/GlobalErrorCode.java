package com.triptrace.global.errorCode;

import com.triptrace.global.app.DomainList;


public enum GlobalErrorCode implements ErrorCode {
    //Default error code & Domain number & message
    INVALID(            "400",    DomainList.GLOBAL,  "올바르지 않은 요청입니다."),
    UNAUTHORIZED(       "401",    DomainList.GLOBAL,  "인증되지 않은 요청입니다."),
    FORBIDDEN(          "403",    DomainList.GLOBAL,  "권한이 없습니다."),
    NOT_FOUND(          "404",    DomainList.GLOBAL,  "해당 데이터가 존재하지 않습니다."),
    DUPLICATE(          "409",    DomainList.GLOBAL,  "중복된 값입니다."),
    PAYLOAD_TOO_LARGE(  "413",    DomainList.GLOBAL,  "파일 크기가 용량을 초과합니다.");

    private final String code;
    private final DomainList tag;
    private final String message;

    GlobalErrorCode(String code, DomainList tag, String message){
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
