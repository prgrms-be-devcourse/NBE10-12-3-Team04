package com.triptrace.domain.auth.auth.service;

import com.triptrace.domain.auth.auth.email.VerificationCodeGenerator;
import com.triptrace.domain.auth.auth.email.VerificationMailSender;
import com.triptrace.domain.auth.auth.entity.EmailVerification;
import com.triptrace.domain.auth.auth.exception.AuthErrorCode;
import com.triptrace.domain.auth.auth.repository.EmailVerificationRepository;
import com.triptrace.global.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 이메일 인증 코드의 발급과 검증을 담당한다.
 * 재발급 시 기존 레코드를 고치지 않고 매번 새 레코드를 쌓으며, 항상 가장 최근 레코드만 유효하게 다룬다.
 */
@Service
public class EmailVerificationService {
    // 확정된 정책값. 환경별로 달라질 여지가 없어 설정이 아닌 상수로 둔다.
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration VERIFIED_VALID_DURATION = Duration.ofMinutes(30);
    private static final int MAX_ATTEMPT_COUNT = 5;

    private final EmailVerificationRepository emailVerificationRepository;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationMailSender mailSender;

    // 메일 발송이 실패하면 레코드도 남기지 않는다. 보내지도 못한 코드로 쿨다운을 소진시키지 않기 위함이다.
    @Transactional
    public void issueCode(String email) {
        LocalDateTime now = LocalDateTime.now();

        emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .filter(latest -> isWithinCooldown(latest, now))
            .ifPresent(latest -> {
                throw new ServiceException(AuthErrorCode.VERIFICATION_CODE_COOLDOWN);
            });

        String code = codeGenerator.generate();

        emailVerificationRepository.save(
            EmailVerification.issue(email, code, now.plus(CODE_TTL))
        );

        mailSender.sendVerificationCode(email, code);
    }

    /**
     * 코드를 검증한다.
     * 이 메서드에 @Transactional을 붙이면 안 된다. 불일치 시 attemptCount를 늘린 뒤 예외를 던지는데,
     * 하나의 트랜잭션으로 묶이면 그 예외가 증가분까지 롤백시켜 시도 횟수 제한이 동작하지 않는다.
     * repository.save()가 각자의 트랜잭션에서 커밋되도록 둔다.
     */
    public void verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationRepository
            .findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new ServiceException(AuthErrorCode.VERIFICATION_CODE_NOT_FOUND));

        if (verification.isVerified()) {
            throw new ServiceException(AuthErrorCode.VERIFICATION_ALREADY_VERIFIED);
        }

        if (verification.isExpired(LocalDateTime.now())) {
            throw new ServiceException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (verification.getAttemptCount() >= MAX_ATTEMPT_COUNT) {
            throw new ServiceException(AuthErrorCode.VERIFICATION_ATTEMPT_EXCEEDED);
        }

        if (!verification.matchesCode(code)) {
            verification.increaseAttemptCount();
            emailVerificationRepository.save(verification);

            throw new ServiceException(AuthErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        verification.verify();
        emailVerificationRepository.save(verification);
    }

    // 회원가입 단계에서 "이 이메일이 방금 인증을 마친 상태인가"를 묻기 위한 헬퍼.
    public boolean isEmailVerified(String email) {
        LocalDateTime now = LocalDateTime.now();

        return emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .filter(EmailVerification::isVerified)
            .map(EmailVerification::getVerifiedAt)
            .filter(verifiedAt -> verifiedAt.plus(VERIFIED_VALID_DURATION).isAfter(now))
            .isPresent();
    }

    private boolean isWithinCooldown(EmailVerification latest, LocalDateTime now) {
        return Optional.ofNullable(latest.getCreatedAt())
            .map(createdAt -> createdAt.plus(RESEND_COOLDOWN).isAfter(now))
            .orElse(false);
    }

    public EmailVerificationService(
        final EmailVerificationRepository emailVerificationRepository,
        final VerificationCodeGenerator codeGenerator,
        final VerificationMailSender mailSender
    ) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.codeGenerator = codeGenerator;
        this.mailSender = mailSender;
    }
}
