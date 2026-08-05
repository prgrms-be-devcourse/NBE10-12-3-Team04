package com.triptrace.domain.member.member.entity

enum class MemberStatus {
    ACTIVE,

    // 소셜 로그인으로 가입했지만 아직 온보딩(프로필 입력)을 마치지 않은 상태
    PENDING_PROFILE,
    WITHDRAWN
}
