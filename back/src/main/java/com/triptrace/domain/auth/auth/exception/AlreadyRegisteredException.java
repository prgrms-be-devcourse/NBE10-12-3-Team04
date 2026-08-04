package com.triptrace.domain.auth.auth.exception;

import com.triptrace.domain.member.member.entity.LoginType;
import com.triptrace.global.exception.ServiceException;

/**
 * 같은 이메일이 다른 경로로 이미 가입돼 있을 때 던진다.
 * 어느 경로로 가입했는지 알려줘야 사용자가 다음 행동을 정할 수 있어 provider를 함께 담는다.
 */
public class AlreadyRegisteredException extends ServiceException {

    private final LoginType provider;

    public AlreadyRegisteredException(LoginType provider) {
        super(
            "%s-%s".formatted(
                AuthErrorCode.ALREADY_REGISTERED.getCode(),
                AuthErrorCode.ALREADY_REGISTERED.getDomain().getCode()
            ),
            "이미 %s로 가입된 이메일입니다.".formatted(toLabel(provider))
        );
        this.provider = provider;
    }

    public LoginType getProvider() {
        return this.provider;
    }

    private static String toLabel(LoginType provider) {
        if (provider == null) {
            return "다른 방식";
        }

        return switch (provider) {
            case LOCAL -> "이메일";
            case GOOGLE -> "구글";
            case KAKAO -> "카카오";
            case NAVER -> "네이버";
        };
    }
}
