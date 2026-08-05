package com.triptrace.domain.member.member.entity

// 회원이 어떤 경로로 가입했는지. LOCAL은 자체 회원가입(이메일+비밀번호), 나머지는 소셜 로그인이다.
enum class LoginType {
    LOCAL,
    GOOGLE,
    KAKAO,
    NAVER
}
