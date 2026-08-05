package com.triptrace.domain.auth.auth.dto

import jakarta.validation.constraints.NotBlank

@JvmRecord
data class GoogleLoginRequest(
    @field:NotBlank
    val code: String?,

    @field:NotBlank
    val redirectUri: String?
)
