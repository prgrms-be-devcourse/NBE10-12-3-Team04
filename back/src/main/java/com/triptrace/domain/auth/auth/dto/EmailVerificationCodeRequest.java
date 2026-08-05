package com.triptrace.domain.auth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationCodeRequest(
    @NotBlank
    @Email
    String email
) {
}
