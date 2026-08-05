package com.triptrace.domain.auth.auth.service;

import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 재발급·로그아웃은 @CookieValue(required = false)로 받은 값을 그대로 넘겨받는다.
 * 즉 쿠키가 없으면 null이 들어오므로, NPE가 아니라 401로 떨어져야 한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthServiceNullTokenTest {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("RT 쿠키 없이 재발급하면 401 예외가 발생한다.")
    void reissueWithoutToken() {
        assertThatThrownBy(() -> authService.reissue(null))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("RT 쿠키 없이 로그아웃하면 401 예외가 발생한다.")
    void logoutWithoutToken() {
        assertThatThrownBy(() -> authService.logout(null))
            .isInstanceOf(ServiceException.class);
    }
}
