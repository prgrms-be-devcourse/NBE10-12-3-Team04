package com.triptrace.domain.image.image.exception

import com.triptrace.global.error.ErrorCode

class ImageProcessException(
    val resultCode: String,
    val msg: String,
    cause: Throwable? = null,
) : RuntimeException(msg, cause) {
    @JvmOverloads
    constructor(errorCode: ErrorCode, message: String = errorCode.message) : this(
        "${errorCode.code}-${errorCode.domain.code}",
        message,
    )
}
