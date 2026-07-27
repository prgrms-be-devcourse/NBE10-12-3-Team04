package com.triptrace.global.errorCode;

import com.triptrace.global.app.DomainNumber;
import com.triptrace.global.log.Loggable;

public enum GlobalErrorCode implements Loggable {
    //Default error code & Domain number & message
    INVALID("400",DomainNumber.GLOBAL, "잘못된 입력입니다."),
    UNAUTHORIZED("401",DomainNumber.GLOBAL, "인증되지 않은 요청입니다."),
    FORBIDDEN("403",DomainNumber.GLOBAL,"권한이 없습니다."),
    NOT_FOUND("404",DomainNumber.GLOBAL,"값을 찾을 수 없습니다."),
    DUPLICATE("409",DomainNumber.GLOBAL,"중복된 값입니다.");

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
