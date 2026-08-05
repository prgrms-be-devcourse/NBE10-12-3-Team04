package com.triptrace.domain.member.member.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// 소셜 가입자의 온보딩 완료 요청. 임시로 발급된 닉네임을 사용자가 정한 값으로 확정한다.
@JvmRecord
data class CompleteProfileRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 50)
    val username: String?
)
