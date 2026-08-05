package com.triptrace.domain.member.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 엔티티가 스스로 지키는 규칙만 확인한다. (DB 제약은 MemberProviderConstraintTest가 담당)
 * 가입 경로에 따라 passwordHash와 providerId 중 한쪽이 비는 구조라, 그 전제를 여기서 고정해 둔다.
 */
class MemberTest {

    @Test
    @DisplayName("소셜 회원은 비밀번호 없이 온보딩 대기 상태로 만들어진다.")
    void createOAuthMember() {
        Member member = Member.ofOAuth("user@gmail.com", LoginType.GOOGLE, "google-sub", "temp-name", "image.jpg");

        assertThat(member.getPasswordHash()).isNull();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING_PROFILE);
        assertThat(member.getProvider()).isEqualTo(LoginType.GOOGLE);
        assertThat(member.getProviderId()).isEqualTo("google-sub");
        assertThat(member.getEmail()).isEqualTo("user@gmail.com");
        assertThat(member.getUsername()).isEqualTo("temp-name");
        assertThat(member.getProfileImageUrl()).isEqualTo("image.jpg");
    }

    @Test
    @DisplayName("일반 회원은 가입 경로가 LOCAL이고 providerId가 없다.")
    void createLocalMember() {
        Member member = new Member("user@test.com", "user", "hashed", null, MemberStatus.ACTIVE);

        assertThat(member.getProvider()).isEqualTo(LoginType.LOCAL);
        assertThat(member.getProviderId()).isNull();
        assertThat(member.getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    @DisplayName("온보딩을 마치면 닉네임이 바뀌고 정상 회원이 된다.")
    void completeProfile() {
        Member member = Member.ofOAuth("user@gmail.com", LoginType.GOOGLE, "google-sub", "temp-name", null);

        member.completeProfile("my-name");

        assertThat(member.getUsername()).isEqualTo("my-name");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("정보 수정 시 넘어온 값만 반영된다.")
    void modifyInfo() {
        Member member = new Member("user@test.com", "old-name", "hashed", "old.jpg", MemberStatus.ACTIVE);

        member.modifyInfo("new-name", "안녕하세요", "new.jpg");

        assertThat(member.getUsername()).isEqualTo("new-name");
        assertThat(member.getIntro()).isEqualTo("안녕하세요");
        assertThat(member.getProfileImageUrl()).isEqualTo("new.jpg");
    }

    @Test
    @DisplayName("정보 수정 시 null로 넘긴 항목은 기존 값을 유지한다.")
    void modifyInfoIgnoresNull() {
        Member member = new Member("user@test.com", "old-name", "hashed", "old.jpg", MemberStatus.ACTIVE);
        member.modifyInfo(null, "기존 소개", null);

        // 부분 수정 API가 보내지 않은 필드를 지워버리면 안 된다.
        member.modifyInfo(null, null, null);

        assertThat(member.getUsername()).isEqualTo("old-name");
        assertThat(member.getIntro()).isEqualTo("기존 소개");
        assertThat(member.getProfileImageUrl()).isEqualTo("old.jpg");
    }

    @Test
    @DisplayName("기본 생성자로 만든 회원은 활성 상태의 LOCAL 회원이다.")
    void defaultValues() {
        // JPA가 사용하는 기본 생성자. 필드 초기값이 유지되는지 확인한다.
        Member member = new Member();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getProvider()).isEqualTo(LoginType.LOCAL);
        assertThat(member.getDeletedAt()).isNull();
    }
}
