package com.triptrace.domain.auth.auth.dto

import jakarta.validation.constraints.NotBlank

@JvmRecord
data class SignupRequest(
    @field:NotBlank
    val email: String?,

    @field:NotBlank
    val username: String?,

    @field:NotBlank
    val password: String?,

    val profileImageUrl: String?
)
