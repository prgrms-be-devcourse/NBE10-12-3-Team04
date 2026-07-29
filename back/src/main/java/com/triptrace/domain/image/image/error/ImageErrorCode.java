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
    FORBIDDEN(DefaultErrorCode.FORBIDDEN),
    IMAGE_PROCESSING_ERROR(DefaultErrorCode.BAD_REQUEST.getCode(),"이미지를 읽을 수 없습니다."),
    IMAGE_PROCESSING_TYPE_ERROR(DefaultErrorCode.BAD_REQUEST.getCode(),"파일 유형이 올바르지 않습니다."),
    IMAGE_PROCESSING_SAVE_ERROR(DefaultErrorCode.BAD_REQUEST.getCode(),"파일을 저장할 수 없습니다."),
    IMAGE_PROCESSING_DELETE_ERROR(DefaultErrorCode.BAD_REQUEST.getCode(),"파일 삭제를 실패했습니다."),
    IMAGE_PROCESSING_READ_ERROR(DefaultErrorCode.BAD_REQUEST.getCode(),"이미지를 읽을 수 없습니다."),
    FILE_EXTRACT_ERROR(DefaultErrorCode.BAD_REQUEST.getCode(),"메타데이터를 추출할 수 없습니다."),
    IMAGE_PROCESSING_REWARD_ERROR(DefaultErrorCode.BAD_REQUEST.getCode(),"보상 트랜잭션 실패했습니다.")
    ;
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
