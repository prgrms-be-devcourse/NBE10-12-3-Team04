package com.triptrace.domain.auth.auth.oauth;

/**
 * 소셜 제공자별 사용자 정보 응답을 회원 가입에 필요한 형태로 통일해 읽는다.
 * 같은 값이라도 응답 안에서의 위치가 제공자마다 달라(구글=sub, 카카오=id, 네이버=response.id)
 * 그 차이는 구현체가 흡수하고, 호출하는 쪽은 이 계약만 본다.
 */
public interface OAuth2UserInfo {

    // 제공자가 발급한 고유 식별자. 회원 식별 키이므로 항상 값이 있어야 한다.
    String getProviderId();

    // email scope를 필수로 요구하므로 항상 값이 있다. 응답에 없으면 구현체가 예외로 막는다.
    String getEmail();

    String getName();

    String getProfileImageUrl();
}
