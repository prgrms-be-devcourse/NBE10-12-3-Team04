package com.triptrace.domain.member.member.repository;

import com.triptrace.domain.member.member.entity.LoginType;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * (provider, provider_id) 조합의 UNIQUE 제약이 DB에 실제로 걸려 있는지 확인한다.
 * 애플리케이션 조회로 걸러지더라도 동시 요청에서는 DB 제약만이 최종 방어선이다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberProviderConstraintTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("같은 provider에 같은 providerId를 가진 회원은 두 명 저장할 수 없다.")
    void rejectsDuplicateProviderAndProviderId() {
        memberRepository.save(oauthMember("first@gmail.com", "user-a", LoginType.GOOGLE, "same-sub"));

        // IDENTITY 전략이라 save() 시점에 INSERT가 나가지만, 전략이 바뀌어도 잡히도록 flush까지 함께 감싼다.
        assertThatThrownBy(() -> {
            memberRepository.save(oauthMember("second@gmail.com", "user-b", LoginType.GOOGLE, "same-sub"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("providerId가 같아도 provider가 다르면 함께 저장할 수 있다.")
    void allowsSameProviderIdAcrossDifferentProviders() {
        memberRepository.save(oauthMember("google@gmail.com", "user-c", LoginType.GOOGLE, "same-sub"));
        memberRepository.save(oauthMember("kakao@gmail.com", "user-d", LoginType.KAKAO, "same-sub"));

        entityManager.flush();

        assertThat(memberRepository.findByProviderAndProviderId(LoginType.GOOGLE, "same-sub")).isPresent();
        assertThat(memberRepository.findByProviderAndProviderId(LoginType.KAKAO, "same-sub")).isPresent();
    }

    @Test
    @DisplayName("providerId가 null인 LOCAL 회원은 여러 명 저장할 수 있다.")
    void allowsMultipleLocalMembersWithNullProviderId() {
        memberRepository.save(localMember("local1@test.com", "local-a"));
        memberRepository.save(localMember("local2@test.com", "local-b"));

        entityManager.flush();

        // UNIQUE 제약은 NULL끼리 충돌시키지 않으므로 기존 LOCAL 회원들이 공존한다.
        assertThat(memberRepository.findByEmail("local1@test.com")).isPresent();
        assertThat(memberRepository.findByEmail("local2@test.com")).isPresent();
    }

    @Test
    @DisplayName("LOCAL 회원과 소셜 회원은 서로 다른 이메일이면 문제없이 공존한다.")
    void allowsLocalAndGoogleMembersSideBySide() {
        memberRepository.save(localMember("mine@test.com", "local-e"));
        memberRepository.save(oauthMember("mine@gmail.com", "google-e", LoginType.GOOGLE, "sub-e"));

        entityManager.flush();

        assertThat(memberRepository.findByEmail("mine@test.com").orElseThrow().getProvider())
            .isEqualTo(LoginType.LOCAL);
        assertThat(memberRepository.findByEmail("mine@gmail.com").orElseThrow().getProvider())
            .isEqualTo(LoginType.GOOGLE);
    }

    @Test
    @DisplayName("이메일이 같으면 가입 경로가 달라도 저장할 수 없다.")
    void rejectsDuplicateEmailAcrossProviders() {
        memberRepository.save(localMember("conflict@test.com", "local-f"));

        assertThatThrownBy(() -> {
            memberRepository.save(oauthMember("conflict@test.com", "google-f", LoginType.GOOGLE, "sub-f"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Member oauthMember(String email, String username, LoginType provider, String providerId) {
        return Member.ofOAuth(email, provider, providerId, username, null);
    }

    private Member localMember(String email, String username) {
        return new Member(email, username, "hashed-password", null, MemberStatus.ACTIVE);
    }
}
