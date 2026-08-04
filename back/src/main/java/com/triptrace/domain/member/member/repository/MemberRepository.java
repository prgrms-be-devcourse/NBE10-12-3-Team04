package com.triptrace.domain.member.member.repository;

import com.triptrace.domain.member.member.entity.LoginType;
import com.triptrace.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByUsername(String username);

    // 소셜 로그인 시 이미 가입된 회원인지 판별한다.
    Optional<Member> findByProviderAndProviderId(LoginType provider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
