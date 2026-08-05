package com.triptrace.domain.image.image.error

import com.triptrace.global.app.Domain
import com.triptrace.global.error.DefaultErrorCode
import com.triptrace.global.error.ErrorCode

enum class ImageErrorCode(private val code: String, private val message: String) : ErrorCode {
    INVALID(DefaultErrorCode.BAD_REQUEST.code, DefaultErrorCode.BAD_REQUEST.message),
    INVALID_TRIP(DefaultErrorCode.BAD_REQUEST.code, "해당 여행기의 이미지가 아닙니다."),
    INVALID_POST(DefaultErrorCode.BAD_REQUEST.code, "해당 포스트의 이미지가 아닙니다."),
    FAIL_SAVE(DefaultErrorCode.BAD_REQUEST.code, "이미지 파일 저장에 실패했습니다."),
    NO_IMAGE(DefaultErrorCode.BAD_REQUEST.code, "업로드할 이미지가 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.code, "이미지를 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN.code, DefaultErrorCode.FORBIDDEN.message),
    IMAGE_PROCESSING_ERROR(DefaultErrorCode.BAD_REQUEST.code, "이미지를 읽을 수 없습니다."),
    TYPE_ERROR(DefaultErrorCode.BAD_REQUEST.code, "파일 유형이 올바르지 않습니다."),
    SAVE_ERROR(DefaultErrorCode.BAD_REQUEST.code, "파일을 저장할 수 없습니다."),
    DELETE_ERROR(DefaultErrorCode.BAD_REQUEST.code, "파일 삭제를 실패했습니다."),
    READ_ERROR(DefaultErrorCode.BAD_REQUEST.code, "이미지를 읽을 수 없습니다."),
    FILE_EXTRACT_ERROR(DefaultErrorCode.BAD_REQUEST.code, "메타데이터를 추출할 수 없습니다."),
    REWARD_TRANSACTION_ERROR(DefaultErrorCode.BAD_REQUEST.code, "보상 트랜잭션 실패했습니다.");
    override fun getCode() = code
    override fun getMessage() = message
    override fun getDomain() = Domain.IMAGE
}
