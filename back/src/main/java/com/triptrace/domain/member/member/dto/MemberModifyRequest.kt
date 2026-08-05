package com.triptrace.domain.member.member.dto

import jakarta.validation.constraints.Size

// 내 정보 부분 수정 요청. 넘어오지 않은(null) 필드는 기존 값을 유지한다.
@JvmRecord
data class MemberModifyRequest(
    @field:Size(min = 2, max = 50)
    val username: String?,

    @field:Size(max = 100)
    val intro: String?,

    @field:Size(max = 500)
    val profileImageUrl: String?
)
