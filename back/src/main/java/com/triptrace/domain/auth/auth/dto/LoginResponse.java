package com.triptrace.domain.auth.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triptrace.domain.member.member.entity.MemberStatus;

public record LoginResponse(
    String accessToken,
    String tokenType,
    // 소셜 로그인은 온보딩 여부(PENDING_PROFILE)를 알려줘야 하지만, LOCAL 로그인 응답은 기존 형태를 그대로 둔다.
    @JsonInclude(JsonInclude.Include.NON_NULL) MemberStatus status
) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer", null);
    }

    public LoginResponse(String accessToken, MemberStatus status) {
        this(accessToken, "Bearer", status);
    }
}
