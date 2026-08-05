package com.triptrace.global.error

// Default error code & Domain number & message
enum class DefaultErrorCode(
    private val code: String,
    private val message: String
) : ErrorCode {
    BAD_REQUEST("400", "올바르지 않은 요청입니다."),
    UNAUTHORIZED("401", "인증되지 않은 요청입니다."),
    FORBIDDEN("403", "권한이 없습니다."),
    NOT_FOUND("404", "해당 데이터가 존재하지 않습니다."),
    CONFLICT("409", "중복된 값입니다."),
    PAYLOAD_TOO_LARGE("413", "파일 크기가 용량을 초과합니다.");

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
