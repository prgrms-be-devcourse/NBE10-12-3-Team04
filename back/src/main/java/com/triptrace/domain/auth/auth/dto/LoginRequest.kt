package com.triptrace.domain.auth.auth.dto

import jakarta.validation.constraints.NotBlank

// 필드를 nullable로 두어야 값이 빠진 요청도 검증 단계에서 400으로 걸린다.
// non-null로 선언하면 Jackson이 null을 넣는 순간 생성자에서 터져 500이 된다.
@JvmRecord
data class LoginRequest(
    @field:NotBlank
    val email: String?,

    @field:NotBlank
    val password: String?
)
