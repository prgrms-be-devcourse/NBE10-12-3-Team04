package com.triptrace.domain.auth.auth.dto

import com.triptrace.domain.member.member.entity.Member
import java.time.LocalDateTime

@JvmRecord
data class SignupResponse(
    val id: Long?,
    val email: String?,
    val username: String?,
    val createdAt: LocalDateTime?
) {
    constructor(member: Member) : this(
        member.getId(),
        member.email,
        member.username,
        member.getCreatedAt()
    )
}
