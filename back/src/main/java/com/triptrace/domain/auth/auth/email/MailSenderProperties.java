package com.triptrace.domain.auth.auth.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 메일 발송에 필요한 애플리케이션 자체 설정.
 * SMTP 접속 정보(spring.mail.*)는 Spring Boot가 바인딩하므로 여기서는 다루지 않는다.
 * 실제 값은 git에 올리지 않는 application-secret.yaml에서 주입된다.
 */
@ConfigurationProperties(prefix = "mail")
public record MailSenderProperties(
    String from
) {
}
