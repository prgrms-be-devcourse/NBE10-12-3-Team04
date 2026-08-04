package com.triptrace.domain.auth.auth.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 기동 시 구글 OAuth 자격 증명이 실제로 주입됐는지만 확인한다.
 * 값 자체는 절대 남기지 않고 "채워졌는지 + 길이"만 기록한다.
 */
@Component
public class GoogleOAuthCredentialsCheck {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthCredentialsCheck.class);

    private final GoogleOAuthProperties properties;

    public GoogleOAuthCredentialsCheck(final GoogleOAuthProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logCredentialStatus() {
        log.info("[google-oauth] clientId={}, clientSecret={}, redirectUri={}",
            describe(properties.clientId()),
            describe(properties.clientSecret()),
            describe(properties.redirectUri()));
    }

    private String describe(String value) {
        if (value == null) {
            return "NULL";
        }

        if (!StringUtils.hasText(value)) {
            return "EMPTY";
        }

        if (value.startsWith("${")) {
            return "UNRESOLVED-PLACEHOLDER";
        }

        return "SET(len=%d)".formatted(value.length());
    }
}
