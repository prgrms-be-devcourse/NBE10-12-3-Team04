package com.triptrace.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotNull

@JvmRecord
data class RsData<T>(
    @field:NotNull val resultCode: String,
    // 응답 본문에는 나가지 않고, 예외 핸들러가 HTTP 상태코드를 정할 때만 쓴다.
    @field:JsonIgnore val statusCode: Int,
    @field:NotNull val msg: String,
    // 데이터가 없는 응답(RsData<Void>)은 null을 담으므로 nullable이다.
    val data: T?
) {
    constructor(resultCode: String, msg: String) : this(resultCode, msg, null)

    // resultCode 앞자리("401-1" → 401)를 그대로 HTTP 상태코드로 쓴다.
    constructor(resultCode: String, msg: String, data: T?) :
        this(resultCode, resultCode.split("-", limit = 2)[0].toInt(), msg, data)
}
