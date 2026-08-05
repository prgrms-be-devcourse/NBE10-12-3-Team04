package com.triptrace.domain.image.image.exception

import com.triptrace.global.error.ErrorCode

class ImageProcessException(
    val resultCode: String,
    val msg: String,
    cause: Throwable? = null,
) : RuntimeException(msg, cause) {
    // ErrorCode가 코틀린 인터페이스가 되면서 getter는 메서드로, 반환 타입은 nullable로 노출된다.
    @JvmOverloads
    constructor(errorCode: ErrorCode, message: String = errorCode.getMessage().orEmpty()) : this(
        "${errorCode.getCode()}-${errorCode.getDomain().code}",
        message,
    )
}
