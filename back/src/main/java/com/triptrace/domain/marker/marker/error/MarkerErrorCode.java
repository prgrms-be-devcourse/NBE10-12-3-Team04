package com.triptrace.domain.marker.marker.error;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.DefaultErrorCode;
import com.triptrace.global.error.ErrorCode;

public enum MarkerErrorCode implements ErrorCode {
    POST_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "게시물을 찾을 수 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "마커를 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN),
    KEYWORD_REQUIRED(DefaultErrorCode.BAD_REQUEST.getCode(), "검색어를 입력해주세요."),
    COORDINATES_REQUIRED(DefaultErrorCode.BAD_REQUEST.getCode(), "좌표를 입력해주세요."),
    DELETE_NOT_ALLOWED(DefaultErrorCode.BAD_REQUEST.getCode(), "마커는 삭제할 수 없습니다.");

    private final String code;
    private final String message;

    MarkerErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    MarkerErrorCode(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
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
        return Domain.MARKER;
    }
}
