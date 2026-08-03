package com.triptrace.domain.auth.auth.oauth;

import com.triptrace.domain.auth.auth.exception.AuthErrorCode;
import com.triptrace.global.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * 구글 OAuth 서버와의 HTTP 통신만 담당한다. (인가 코드 → 액세스 토큰 → 사용자 정보)
 * 장소/지오코딩 클라이언트와 달리 실패를 빈 값으로 넘기지 않는다.
 * 토큰 교환이나 사용자 정보 조회가 실패하면 로그인 자체가 성립할 수 없기 때문이다.
 */
@Component
public class GoogleOAuthClient {

    private static final String TOKEN_BASE_URL = "https://oauth2.googleapis.com";
    private static final String USER_INFO_BASE_URL = "https://www.googleapis.com";
    private static final String GRANT_TYPE = "authorization_code";

    private final GoogleOAuthProperties properties;
    private final RestClient tokenClient;
    private final RestClient userInfoClient;

    @Autowired
    public GoogleOAuthClient(GoogleOAuthProperties properties) {
        this(
            properties,
            RestClient.builder().baseUrl(TOKEN_BASE_URL).build(),
            RestClient.builder().baseUrl(USER_INFO_BASE_URL).build()
        );
    }

    GoogleOAuthClient(GoogleOAuthProperties properties, RestClient tokenClient, RestClient userInfoClient) {
        this.properties = properties;
        this.tokenClient = tokenClient;
        this.userInfoClient = userInfoClient;
    }

    // 인가 코드를 액세스 토큰으로 교환한다. redirectUri는 인가 요청 때 쓴 값과 같아야 구글이 받아준다.
    public String exchangeToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", GRANT_TYPE);

        JsonNode response = post(form);
        String accessToken = response == null ? null : response.path("access_token").asString(null);

        if (!StringUtils.hasText(accessToken)) {
            throw new ServiceException(AuthErrorCode.GOOGLE_AUTH_FAILED);
        }

        return accessToken;
    }

    public JsonNode fetchUserInfo(String accessToken) {
        JsonNode response = get(accessToken);

        if (response == null) {
            throw new ServiceException(AuthErrorCode.GOOGLE_AUTH_FAILED);
        }

        return response;
    }

    private JsonNode post(MultiValueMap<String, String> form) {
        try {
            return tokenClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        } catch (RestClientException e) {
            // 만료·재사용된 인가 코드나 잘못된 자격 증명은 4xx로 돌아온다. 원인은 로그로 남기고 응답에는 노출하지 않는다.
            throw new ServiceException(AuthErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    private JsonNode get(String accessToken) {
        try {
            return userInfoClient.get()
                .uri("/oauth2/v3/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new ServiceException(AuthErrorCode.GOOGLE_AUTH_FAILED);
        }
    }
}
