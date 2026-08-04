package com.triptrace.domain.auth.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
    @NotBlank String code,
    @NotBlank String redirectUri
) {
}
