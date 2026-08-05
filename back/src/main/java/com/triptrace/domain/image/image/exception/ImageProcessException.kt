package com.triptrace.domain.image.image.exception

import com.triptrace.global.error.ErrorCode

class ImageProcessException : RuntimeException {
    val resultCode: String
    val msg: String
    constructor(resultCode: String, message: String, cause: Throwable? = null) : super(message, cause) { this.resultCode = resultCode; msg = message }
    @JvmOverloads constructor(errorCode: ErrorCode, message: String = errorCode.message) : super(message) { resultCode = "${errorCode.code}-${errorCode.domain.code}"; msg = message }
}
