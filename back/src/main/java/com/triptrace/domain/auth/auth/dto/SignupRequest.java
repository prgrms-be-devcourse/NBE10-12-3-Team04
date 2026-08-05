package com.triptrace.domain.auth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
    @NotBlank
    @Email
    String email,

    @NotBlank
    String username,

    @NotBlank
    String password,

    String profileImageUrl
) {
}
