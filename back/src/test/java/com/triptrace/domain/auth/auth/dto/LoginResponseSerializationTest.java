package com.triptrace.domain.auth.auth.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptrace.domain.member.member.entity.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로그인 응답의 JSON 모양이 바뀌지 않았는지 확인한다.
 * status는 소셜 로그인에서만 내려가야 하므로, LOCAL 로그인 응답에 키가 생기면 기존 클라이언트 계약이 깨진다.
 */
class LoginResponseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("status가 없으면 JSON에 status 키 자체가 나오지 않는다.")
    void omitNullStatus() throws Exception {
        String json = objectMapper.writeValueAsString(new LoginResponse("access-token"));

        assertThat(json).doesNotContain("status");
        assertThat(json).contains("\"accessToken\":\"access-token\"");
        assertThat(json).contains("\"tokenType\":\"Bearer\"");
    }

    @Test
    @DisplayName("status가 있으면 JSON에 그대로 포함된다.")
    void includeStatus() throws Exception {
        String json =
            objectMapper.writeValueAsString(new LoginResponse("access-token", MemberStatus.PENDING_PROFILE));

        assertThat(json).contains("\"status\":\"PENDING_PROFILE\"");
    }

    @Test
    @DisplayName("재발급 응답은 accessToken과 tokenType만 가진다.")
    void reissueResponseShape() throws Exception {
        String json = objectMapper.writeValueAsString(new ReissueResponse("new-token"));

        assertThat(json).isEqualTo("{\"accessToken\":\"new-token\",\"tokenType\":\"Bearer\"}");
    }
}
