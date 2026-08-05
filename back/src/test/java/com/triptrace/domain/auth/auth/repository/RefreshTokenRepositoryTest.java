package com.triptrace.domain.auth.auth.repository;

import com.triptrace.domain.auth.auth.entity.RefreshToken;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 재발급·로그아웃이 기대는 조회/삭제 메서드가 실제 DB에서 의도대로 동작하는지 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("토큰 문자열로 저장된 리프레시 토큰을 찾을 수 있다.")
    void findByToken() {
        Member member = saveMember("owner@test.com", "owner");
        refreshTokenRepository.save(new RefreshToken(member, "token-a", LocalDateTime.now().plusDays(7)));

        assertThat(refreshTokenRepository.findByToken("token-a")).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 토큰을 찾으면 비어 있는 결과가 나온다.")
    void findByTokenNotFound() {
        assertThat(refreshTokenRepository.findByToken("unknown")).isEmpty();
    }

    @Test
    @DisplayName("한 회원이 발급받은 토큰을 모두 조회한다.")
    void findAllByMember() {
        Member member = saveMember("multi@test.com", "multi");
        Member other = saveMember("other@test.com", "other");
        refreshTokenRepository.save(new RefreshToken(member, "token-b", LocalDateTime.now().plusDays(7)));
        refreshTokenRepository.save(new RefreshToken(member, "token-c", LocalDateTime.now().plusDays(7)));
        refreshTokenRepository.save(new RefreshToken(other, "token-d", LocalDateTime.now().plusDays(7)));

        List<RefreshToken> tokens = refreshTokenRepository.findAllByMember(member);

        // 다른 회원의 토큰이 섞이면 로그아웃 시 남의 세션까지 끊긴다.
        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(RefreshToken::getToken).containsExactlyInAnyOrder("token-b", "token-c");
    }

    @Test
    @DisplayName("회원의 토큰을 한 번에 삭제할 수 있다.")
    void deleteAllByMember() {
        Member member = saveMember("bye@test.com", "bye");
        Member other = saveMember("stay@test.com", "stay");
        refreshTokenRepository.save(new RefreshToken(member, "token-e", LocalDateTime.now().plusDays(7)));
        refreshTokenRepository.save(new RefreshToken(other, "token-f", LocalDateTime.now().plusDays(7)));

        refreshTokenRepository.deleteAllByMember(member);
        entityManager.flush();

        assertThat(refreshTokenRepository.findAllByMember(member)).isEmpty();
        assertThat(refreshTokenRepository.findAllByMember(other)).hasSize(1);
    }

    @Test
    @DisplayName("폐기 표시는 다시 조회해도 유지된다.")
    void revokeIsPersisted() {
        Member member = saveMember("revoke@test.com", "revoke");
        RefreshToken saved =
            refreshTokenRepository.save(new RefreshToken(member, "token-g", LocalDateTime.now().plusDays(7)));

        saved.revoke();
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findByToken("token-g").orElseThrow().isRevoked()).isTrue();
    }

    @Test
    @DisplayName("같은 토큰 문자열은 두 번 저장할 수 없다.")
    void rejectDuplicateToken() {
        Member member = saveMember("dup@test.com", "dup");
        refreshTokenRepository.save(new RefreshToken(member, "token-h", LocalDateTime.now().plusDays(7)));

        assertThatThrownBy(() -> {
            refreshTokenRepository.save(new RefreshToken(member, "token-h", LocalDateTime.now().plusDays(7)));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Member saveMember(String email, String username) {
        return memberRepository.save(new Member(email, username, "hashed-password", null, MemberStatus.ACTIVE));
    }
}
