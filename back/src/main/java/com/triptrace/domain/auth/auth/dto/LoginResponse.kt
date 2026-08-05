package com.triptrace.domain.auth.auth.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.triptrace.domain.member.member.entity.MemberStatus

@JvmRecord
data class LoginResponse(
    val accessToken: String?,
    val tokenType: String?,
    // 소셜 로그인은 온보딩 여부(PENDING_PROFILE)를 알려줘야 하지만, LOCAL 로그인 응답은 기존 형태를 그대로 둔다.
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val status: MemberStatus?
) {
    constructor(accessToken: String?) : this(accessToken, "Bearer", null)

    constructor(accessToken: String?, status: MemberStatus?) : this(accessToken, "Bearer", status)
}
