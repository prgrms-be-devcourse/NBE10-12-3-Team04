package com.triptrace.domain.image.image.error

import com.triptrace.global.app.Domain
import com.triptrace.global.error.DefaultErrorCode
import com.triptrace.global.error.ErrorCode

enum class ImageErrorCode(private val code: String, private val message: String) : ErrorCode {
    INVALID("400", "올바르지 않은 요청입니다."),
    INVALID_TRIP("400", "해당 여행기의 이미지가 아닙니다."),
    INVALID_POST("400", "해당 포스트의 이미지가 아닙니다."),
    FAIL_SAVE( "400", "이미지 파일 저장에 실패했습니다."),
    NO_IMAGE( "400", "업로드할 이미지가 없습니다."),
    NOT_FOUND("404", "이미지를 찾을 수 없습니다."),
    FORBIDDEN("403", "권한이 없습니다."),
    IMAGE_PROCESSING_ERROR( "400", "이미지를 읽을 수 없습니다."),
    TYPE_ERROR( "400", "파일 유형이 올바르지 않습니다."),
    SAVE_ERROR( "400", "파일을 저장할 수 없습니다."),
    DELETE_ERROR( "400", "파일 삭제를 실패했습니다."),
    READ_ERROR( "400", "이미지를 읽을 수 없습니다."),
    FILE_EXTRACT_ERROR( "400", "메타데이터를 추출할 수 없습니다."),
    REWARD_TRANSACTION_ERROR( "400", "보상 트랜잭션 실패했습니다.");
    override fun getCode() = code
    override fun getMessage() = message
    override fun getDomain() = Domain.IMAGE
}
