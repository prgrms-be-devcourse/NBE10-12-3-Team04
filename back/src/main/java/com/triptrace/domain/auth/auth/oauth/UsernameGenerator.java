package com.triptrace.domain.auth.auth.oauth;

import com.triptrace.domain.auth.auth.exception.AuthErrorCode;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.global.exception.ServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

/**
 * 소셜 가입자에게 붙일 임시 닉네임을 만든다. (이메일 앞부분 + 랜덤 4자리)
 * 닉네임은 UNIQUE라 충돌할 수 있으므로 비어 있는 값을 찾을 때까지 정해진 횟수만큼 다시 뽑는다.
 */
@Component
public class UsernameGenerator {

    private static final int MAX_ATTEMPTS = 5;
    private static final int RANDOM_BOUND = 10000;
    private static final int MAX_PREFIX_LENGTH = 20;
    private static final String FALLBACK_PREFIX = "user";

    private final MemberRepository memberRepository;
    private final SecureRandom random = new SecureRandom();

    public UsernameGenerator(final MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public String generate(String email) {
        String prefix = toPrefix(email);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = "%s%04d".formatted(prefix, random.nextInt(RANDOM_BOUND));

            if (!memberRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        throw new ServiceException(AuthErrorCode.USERNAME_GENERATION_FAILED);
    }

    // 이메일 로컬파트에서 닉네임에 쓸 수 있는 문자만 남긴다. 남는 게 없으면 고정 접두사로 대체한다.
    private String toPrefix(String email) {
        if (!StringUtils.hasText(email)) {
            return FALLBACK_PREFIX;
        }

        String localPart = email.split("@", 2)[0];
        String sanitized = localPart.replaceAll("[^a-zA-Z0-9]", "");

        if (!StringUtils.hasText(sanitized)) {
            return FALLBACK_PREFIX;
        }

        if (sanitized.length() > MAX_PREFIX_LENGTH) {
            return sanitized.substring(0, MAX_PREFIX_LENGTH);
        }

        return sanitized;
    }
}
