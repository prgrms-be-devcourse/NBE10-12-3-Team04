package com.triptrace.domain.auth.auth.oauth;

import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

/**
 * 구글 userinfo 엔드포인트(https://www.googleapis.com/oauth2/v3/userinfo) 응답을 읽는다.
 * 응답 예: {"sub": "1234567890", "email": "user@example.com", "email_verified": true,
 *          "name": "홍길동", "picture": "https://..."}
 */
public final class GoogleUserInfo implements OAuth2UserInfo {

    private final String providerId;
    private final String email;
    private final boolean emailVerified;
    private final String name;
    private final String profileImageUrl;

    private GoogleUserInfo(
        String providerId,
        String email,
        boolean emailVerified,
        String name,
        String profileImageUrl
    ) {
        this.providerId = providerId;
        this.email = email;
        this.emailVerified = emailVerified;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public static GoogleUserInfo from(JsonNode attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("구글 사용자 정보 응답이 비어 있습니다.");
        }

        // sub(회원 식별 키)와 email(필수 scope)은 없으면 안 되는 값이라 조용히 넘기지 않고 예외로 드러낸다.
        // name/picture는 빠져도 가입을 막을 이유가 없어 null을 허용한다.
        String sub = attributes.path("sub").asString(null);
        if (!StringUtils.hasText(sub)) {
            throw new IllegalArgumentException("구글 사용자 정보 응답에 sub가 없습니다.");
        }

        String email = attributes.path("email").asString(null);
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("구글 사용자 정보 응답에 email이 없습니다.");
        }

        return new GoogleUserInfo(
            sub,
            email,
            attributes.path("email_verified").asBoolean(false),
            attributes.path("name").asString(null),
            attributes.path("picture").asString(null)
        );
    }

    @Override
    public String getProviderId() {
        return this.providerId;
    }

    @Override
    public String getEmail() {
        return this.email;
    }

    // 구글이 이메일 소유를 확인했는지 여부. 값만 노출하고, 이걸로 막을지 말지는 정책을 아는 쪽에서 판단한다.
    public boolean isEmailVerified() {
        return this.emailVerified;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getProfileImageUrl() {
        return this.profileImageUrl;
    }
}
