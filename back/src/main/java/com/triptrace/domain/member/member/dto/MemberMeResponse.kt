package com.triptrace.domain.member.member.dto

import com.triptrace.domain.member.member.entity.Member
import com.triptrace.domain.member.member.entity.MemberStatus
import java.time.LocalDateTime

@JvmRecord
data class MemberMeResponse(
    val id: Long?,
    val email: String?,
    val username: String?,
    val nickname: String?,
    val intro: String?,
    val profileImageUrl: String?,
    val status: MemberStatus?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    constructor(member: Member) : this(
        member.getId(),
        member.getEmail(),
        member.getUsername(),
        // 프론트가 nickname 키를 쓰고 있어 username과 같은 값을 함께 내려준다.
        member.getUsername(),
        member.getIntro(),
        member.getProfileImageUrl(),
        member.getStatus(),
        member.getCreatedAt(),
        member.getUpdatedAt()
    )
}
