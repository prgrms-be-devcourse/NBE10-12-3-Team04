package com.triptrace.domain.image.image.error;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.DefaultErrorCode;
import com.triptrace.global.error.ErrorCode;

public enum ImageErrorCode implements ErrorCode {
    INVALID(DefaultErrorCode.BAD_REQUEST),
    INVALID_TRIP(DefaultErrorCode.BAD_REQUEST.getCode(), "해당 여행기의 이미지가 아닙니다."),
    INVALID_POST(DefaultErrorCode.BAD_REQUEST.getCode(), "해당 포스트의 이미지가 아닙니다."),
    FAIL_SAVE(DefaultErrorCode.BAD_REQUEST.getCode(),"이미지 파일 저장에 실패했습니다."),
    NO_IMAGE(DefaultErrorCode.BAD_REQUEST.getCode(), "업로드할 이미지가 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "이미지를 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN);

    private String code;
    private String message;
    private ImageErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    private ImageErrorCode(ErrorCode errorCode) {
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
        return Domain.IMAGE;
    }
}
