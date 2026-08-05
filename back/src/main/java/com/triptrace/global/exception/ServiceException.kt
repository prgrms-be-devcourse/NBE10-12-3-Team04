package com.triptrace.global.exception

import com.triptrace.global.app.Domain
import com.triptrace.global.error.ErrorCode
import com.triptrace.global.rsData.RsData

// AlreadyRegisteredException이 자바에서 상속하므로 open으로 둔다.
open class ServiceException : RuntimeException {

    private val resultCode: String
    private val msg: String

    constructor(resultCode: String, msg: String) : super("$resultCode : $msg") {
        this.resultCode = resultCode
        this.msg = msg
    }

    constructor(errorCode: ErrorCode) : this(errorCode, errorCode.getDomain())

    constructor(errorCode: ErrorCode, domain: Domain) : this(
        "${errorCode.getCode()}-${domain.code}",
        errorCode.getMessage().orEmpty()
    )

    fun getRsData(): RsData<Void> = RsData(resultCode, msg, null)
}
