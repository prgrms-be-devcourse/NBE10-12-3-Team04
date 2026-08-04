package com.triptrace.domain.auth.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 구글 OAuth 클라이언트 자격 증명. 실제 값은 git에 올리지 않는 application-secret.yaml에서 주입된다.
 */
@ConfigurationProperties(prefix = "oauth.google")
public record GoogleOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri
) {
}
