package com.triptrace.domain.auth.auth.repository

import com.triptrace.domain.auth.auth.entity.RefreshToken
import com.triptrace.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    // RT 쿠키가 없는 요청도 여기까지 흘러온다. null이면 매칭되는 행이 없어 빈 Optional이 나온다.
    fun findByToken(token: String?): Optional<RefreshToken>

    fun findAllByMember(member: Member): List<RefreshToken>

    fun deleteAllByMember(member: Member)
}
