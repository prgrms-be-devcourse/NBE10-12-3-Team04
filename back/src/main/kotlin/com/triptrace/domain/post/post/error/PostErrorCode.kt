package com.triptrace.domain.post.post.error

import com.triptrace.global.app.Domain
import com.triptrace.global.error.DefaultErrorCode
import com.triptrace.global.error.ErrorCode

enum class PostErrorCode(
    private val code: String,
    private val message: String,
) : ErrorCode {
    TRIP_NOT_FOUND(DefaultErrorCode.NOT_FOUND.code, "여행기를 찾을 수 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.code, "게시물을 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN.code, "여행기에 대한 권한이 없습니다."),
    ;

    override fun getCode(): String = code

    override fun getMessage(): String = message

    override fun getDomain(): Domain = Domain.POST
}
