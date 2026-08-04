package com.triptrace.domain.auth.auth.dto;

import com.triptrace.domain.member.member.entity.MemberStatus;

/**
 * 소셜 로그인 결과. RT는 컨트롤러가 쿠키로 내려주고, status는 온보딩 화면으로 보낼지 판단하는 데 쓴다.
 */
public record OAuthLoginResult(
    TokenPair tokens,
    MemberStatus status
) {
}
