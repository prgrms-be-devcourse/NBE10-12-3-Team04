package com.triptrace.domain.auth.auth.email;

import com.triptrace.domain.auth.auth.exception.AuthErrorCode;
import com.triptrace.global.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 인증 코드 메일 발송만 담당한다.
 * 발송 실패 원인(인증 실패·연결 실패 등)은 서버 로그에만 남기고,
 * 클라이언트에는 원인을 숨긴 채 EMAIL_SEND_FAILED만 내려준다.
 */
@Component
public class VerificationMailSender {

    private static final Logger log = LoggerFactory.getLogger(VerificationMailSender.class);

    private static final String SUBJECT = "[TripTrace] 이메일 인증 코드";
    private static final String BODY_FORMAT = """
        TripTrace 이메일 인증 코드는 %s 입니다.
        코드는 발급 후 5분간 유효합니다.""";

    private final JavaMailSender mailSender;
    private final MailSenderProperties properties;

    public VerificationMailSender(final JavaMailSender mailSender, final MailSenderProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        // from이 비어 있으면 SMTP 계정 주소가 그대로 발신자가 된다. 빈 문자열을 그대로 넘기면 발송이 거부된다.
        if (StringUtils.hasText(properties.from())) {
            message.setFrom(properties.from());
        }

        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText(BODY_FORMAT.formatted(code));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // 원인은 SMTP 자격 증명 오류·네트워크 문제 등 다양한데, 어느 쪽이든 클라이언트가 할 수 있는 일이 없다.
            // 코드 값이 로그로 새지 않도록 수신자와 예외 메시지만 남긴다.
            log.warn("[email-verification] 인증 메일 발송 실패: to={}, cause={}", to, e.getMessage(), e);

            throw new ServiceException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
