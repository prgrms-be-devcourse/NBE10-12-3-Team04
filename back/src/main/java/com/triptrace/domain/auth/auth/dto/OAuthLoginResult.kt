package com.triptrace.domain.auth.auth.dto

import com.triptrace.domain.member.member.entity.MemberStatus

/**
 * 소셜 로그인 결과. RT는 컨트롤러가 쿠키로 내려주고, status는 온보딩 화면으로 보낼지 판단하는 데 쓴다.
 *
 * 요청 DTO와 달리 서비스가 직접 만들어 컨트롤러로 넘기는 내부 객체다.
 * Jackson이 값을 채우는 경로가 없으므로 두 필드 모두 non-null로 둔다.
 */
@JvmRecord
data class OAuthLoginResult(
    val tokens: TokenPair,
    val status: MemberStatus
)
