package com.triptrace.domain.auth.auth.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleUserInfoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("구글 응답의 sub/email/name/picture를 계약에 맞게 읽는다.")
    void from() {
        GoogleUserInfo userInfo = GoogleUserInfo.from(attributes("1234567890", "user@example.com", true));

        assertThat(userInfo.getProviderId()).isEqualTo("1234567890");
        assertThat(userInfo.getEmail()).isEqualTo("user@example.com");
        assertThat(userInfo.isEmailVerified()).isTrue();
        assertThat(userInfo.getName()).isEqualTo("홍길동");
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://example.com/p.png");
    }

    @Test
    @DisplayName("email_verified가 false면 그대로 노출한다. (차단 여부는 호출하는 쪽이 판단)")
    void fromUnverifiedEmail() {
        GoogleUserInfo userInfo = GoogleUserInfo.from(attributes("1234567890", "user@example.com", false));

        assertThat(userInfo.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("email_verified 필드가 아예 없으면 미인증으로 본다.")
    void fromMissingEmailVerified() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sub", "1234567890");
        node.put("email", "user@example.com");

        assertThat(GoogleUserInfo.from(node).isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("name과 picture는 없어도 null로 담긴다.")
    void fromWithoutOptionalFields() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sub", "1234567890");
        node.put("email", "user@example.com");
        node.put("email_verified", true);

        GoogleUserInfo userInfo = GoogleUserInfo.from(node);

        assertThat(userInfo.getName()).isNull();
        assertThat(userInfo.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("sub가 없으면 회원 식별이 불가능하므로 예외를 던진다.")
    void fromMissingSub() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("email", "user@example.com");

        assertThatThrownBy(() -> GoogleUserInfo.from(node))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("구글 사용자 정보 응답에 sub가 없습니다.");
    }

    @Test
    @DisplayName("email scope를 필수로 요구하므로 email이 없으면 예외를 던진다.")
    void fromMissingEmail() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sub", "1234567890");

        assertThatThrownBy(() -> GoogleUserInfo.from(node))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("구글 사용자 정보 응답에 email이 없습니다.");
    }

    @Test
    @DisplayName("응답 자체가 null이면 예외를 던진다.")
    void fromNull() {
        assertThatThrownBy(() -> GoogleUserInfo.from(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("구글 사용자 정보 응답이 비어 있습니다.");
    }

    private ObjectNode attributes(String sub, String email, boolean emailVerified) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sub", sub);
        node.put("email", email);
        node.put("email_verified", emailVerified);
        node.put("name", "홍길동");
        node.put("picture", "https://example.com/p.png");

        return node;
    }
}
