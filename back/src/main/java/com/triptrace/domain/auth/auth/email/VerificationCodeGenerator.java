package com.triptrace.domain.auth.auth.email;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 이메일 인증용 6자리 숫자 코드를 만든다.
 * 000000~999999 범위라 앞자리 0이 나올 수 있어 항상 6자리로 채운 문자열로 다룬다.
 */
@Component
public class VerificationCodeGenerator {
    private static final int CODE_BOUND = 1_000_000;
    private static final String CODE_FORMAT = "%06d";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return CODE_FORMAT.formatted(random.nextInt(CODE_BOUND));
    }
}
