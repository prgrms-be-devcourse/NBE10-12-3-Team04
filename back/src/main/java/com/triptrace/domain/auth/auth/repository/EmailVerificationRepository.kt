package com.triptrace.domain.auth.auth.repository

import com.triptrace.domain.auth.auth.entity.EmailVerification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface EmailVerificationRepository : JpaRepository<EmailVerification, Long> {

    fun findTopByEmailOrderByCreatedAtDesc(email: String): Optional<EmailVerification>
}
