package com.triptrace.domain.auth.auth.entity;

import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 이메일 인증 코드 발급 이력. 재발급 시 새 레코드를 쌓고 가장 최근 것만 유효하게 다룬다.
 * 발급 시각(createdAt)은 BaseEntity의 JPA Auditing이 채운다.
 */
@Entity
@Table(
    name = "email_verification",
    indexes = @Index(name = "idx_email_verification_email", columnList = "email")
)
public class EmailVerification extends BaseEntity {
    @Column(nullable = false)
    private String email;

    @Column(length = 6, nullable = false)
    private String code;

    @Column(nullable = false)
    private boolean verified = false;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attemptCount = 0;

    private EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification issue(String email, String code, LocalDateTime expiresAt) {
        return new EmailVerification(email, code, expiresAt);
    }

    // 인증 성공. 이후 회원가입 유효기간은 이 verifiedAt을 기준으로 계산한다.
    public void verify() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    // 코드 불일치 시 호출. 정해진 횟수를 넘기면 이 코드는 더 이상 검증에 쓸 수 없다.
    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    public boolean isExpired(LocalDateTime now) {
        return this.expiresAt.isBefore(now);
    }

    public boolean matchesCode(String code) {
        return this.code.equals(code);
    }

    public String getEmail() {
        return this.email;
    }

    public String getCode() {
        return this.code;
    }

    public boolean isVerified() {
        return this.verified;
    }

    public LocalDateTime getVerifiedAt() {
        return this.verifiedAt;
    }

    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    public int getAttemptCount() {
        return this.attemptCount;
    }

    protected EmailVerification() {
    }
}
