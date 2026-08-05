package com.triptrace.domain.auth.auth.repository

import com.triptrace.domain.auth.auth.entity.RefreshToken
import com.triptrace.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByToken(token: String): Optional<RefreshToken>

    fun findAllByMember(member: Member): List<RefreshToken>

    fun deleteAllByMember(member: Member)
}
