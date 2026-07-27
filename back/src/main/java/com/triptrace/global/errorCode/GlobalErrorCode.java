package com.triptrace.global.errorCode;

public enum GlobalErrorCode implements ErrorCode {
    //Default error code & message
    INVALID("400","1", "잘못된 입력입니다."),
    UNAUTHORIZED("401","1", "인증되지 않은 요청입니다."),
    FORBIDDEN("403","1","권한이 없습니다."),
    NOT_FOUND("404","1","값을 찾을 수 없습니다."),
    DUPLICATE("409","1","중복된 값입니다.");

    private final String code;
    private final String tag;
    private final String message;

    GlobalErrorCode(String code, String tag, String message){
        this.code = code;
        this.tag = tag;
        this.message = message;
    }
    @Override
    public String getCode() {
        return "%s-%s".formatted(code, tag);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
