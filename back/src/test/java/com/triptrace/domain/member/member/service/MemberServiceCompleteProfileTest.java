package com.triptrace.domain.member.member.service;

import com.triptrace.domain.member.member.entity.LoginType;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberServiceCompleteProfileTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("온보딩을 완료하면 닉네임이 확정되고 상태가 ACTIVE로 바뀐다.")
    void completeProfile() {
        Member member = savePendingMember("pending@gmail.com", "temp1234", "sub-1");

        Member result = memberService.completeProfile(member.getId(), "확정닉네임");

        assertThat(result.getUsername()).isEqualTo("확정닉네임");
        assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("임시 닉네임을 그대로 확정해도 본인 값이므로 중복으로 보지 않는다.")
    void completeProfileWithOwnUsername() {
        Member member = savePendingMember("keep@gmail.com", "temp5678", "sub-2");

        Member result = memberService.completeProfile(member.getId(), "temp5678");

        assertThat(result.getUsername()).isEqualTo("temp5678");
        assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("이미 온보딩을 마친 회원이 다시 호출하면 409 예외가 발생한다.")
    void completeProfileTwice() {
        Member member = savePendingMember("twice@gmail.com", "temp0001", "sub-3");
        memberService.completeProfile(member.getId(), "첫번째닉네임");

        assertThatThrownBy(() -> memberService.completeProfile(member.getId(), "두번째닉네임"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-05 : 이미 온보딩이 완료된 회원입니다.");
    }

    @Test
    @DisplayName("처음부터 ACTIVE인 회원은 온보딩을 완료할 수 없다.")
    void completeProfileRejectsActiveMember() {
        Member member = memberRepository.save(
            new Member("active@test.com", "activeuser", "hashed", null, MemberStatus.ACTIVE)
        );

        assertThatThrownBy(() -> memberService.completeProfile(member.getId(), "새닉네임"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-05 : 이미 온보딩이 완료된 회원입니다.");
    }

    @Test
    @DisplayName("다른 회원이 쓰고 있는 닉네임으로는 온보딩을 완료할 수 없다.")
    void completeProfileRejectsDuplicateUsername() {
        memberRepository.save(new Member("owner@test.com", "선점닉네임", "hashed", null, MemberStatus.ACTIVE));
        Member member = savePendingMember("dup@gmail.com", "temp9999", "sub-4");

        assertThatThrownBy(() -> memberService.completeProfile(member.getId(), "선점닉네임"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-1 : 이미 사용중인 닉네임입니다.");

        // 실패했으면 상태도 그대로여야 한다.
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getStatus())
            .isEqualTo(MemberStatus.PENDING_PROFILE);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 404 예외가 발생한다.")
    void completeProfileMemberNotFound() {
        assertThatThrownBy(() -> memberService.completeProfile(-1L, "닉네임"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("404-1 : 회원을 찾을 수 없습니다.");
    }

    private Member savePendingMember(String email, String username, String providerId) {
        return memberRepository.save(
            Member.ofOAuth(email, LoginType.GOOGLE, providerId, username, null)
        );
    }
}
