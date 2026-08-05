package com.triptrace.domain.auth.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@JvmRecord
data class EmailVerifyRequest(
    @field:NotBlank
    @field:Email
    val email: String?,

    @field:NotBlank
    val code: String?
)
