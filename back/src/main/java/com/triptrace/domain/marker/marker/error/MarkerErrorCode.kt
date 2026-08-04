package com.triptrace.domain.marker.marker.error

import com.triptrace.global.app.Domain
import com.triptrace.global.error.DefaultErrorCode
import com.triptrace.global.error.ErrorCode

enum class MarkerErrorCode(private val code: String?, private val message: String?) : ErrorCode {
    POST_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "게시물을 찾을 수 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "마커를 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN),
    KEYWORD_REQUIRED(DefaultErrorCode.BAD_REQUEST.getCode(), "검색어를 입력해주세요."),
    COORDINATES_REQUIRED(DefaultErrorCode.BAD_REQUEST.getCode(), "좌표를 입력해주세요."),
    DELETE_NOT_ALLOWED(DefaultErrorCode.BAD_REQUEST.getCode(), "마커는 삭제할 수 없습니다.");

    constructor(errorCode: ErrorCode) : this(errorCode.getCode(), errorCode.getMessage())

    override fun getCode(): String? {
        return code
    }

    override fun getMessage(): String? {
        return message
    }

    override fun getDomain(): Domain {
        return Domain.MARKER
    }
}
