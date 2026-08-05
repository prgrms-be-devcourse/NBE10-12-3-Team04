package com.triptrace.domain.member.member.repository

import com.triptrace.domain.member.member.entity.LoginType
import com.triptrace.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {

    fun findByEmail(email: String): Optional<Member>

    fun findByUsername(username: String): Optional<Member>

    // 소셜 로그인 시 이미 가입된 회원인지 판별한다.
    fun findByProviderAndProviderId(provider: LoginType, providerId: String): Optional<Member>

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean
}
