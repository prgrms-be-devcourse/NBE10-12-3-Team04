package com.triptrace.domain.post.post.error

import com.triptrace.global.app.Domain
import com.triptrace.global.error.DefaultErrorCode
import com.triptrace.global.error.ErrorCode

enum class PostErrorCode(code: String, message: String) : ErrorCode {
    TRIP_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "여행기를 찾을 수 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "게시물을 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN.getCode(), "여행기에 대한 권한이 없습니다.");

    private val code: String?
    private val message: String?

    init {
        this.code = code
        this.message = message
    }

    override fun getCode(): String? {
        return code
    }

    override fun getMessage(): String? {
        return message
    }

    override fun getDomain(): Domain {
        return Domain.POST
    }
}
