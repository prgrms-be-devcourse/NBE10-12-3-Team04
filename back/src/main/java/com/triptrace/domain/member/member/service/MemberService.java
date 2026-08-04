package com.triptrace.domain.member.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.triptrace.domain.member.member.dto.MemberModifyRequest;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.exception.MemberErrorCode;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.global.exception.ServiceException;

/**
 * 회원 데이터의 규칙과 영속화를 담당한다. (중복 검사 → 저장, 이메일로 조회)
 * 비밀번호 해시·토큰 발급 같은 인증 조립은 AuthService의 몫이며, 여기선 받은 값을 다룰 뿐이다.
 */
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    // 회원가입: 이메일/닉네임 중복을 막고 새 회원을 저장한다. (passwordHash는 이미 해시된 값)
    @Transactional
    public Member signup(String email, String username, String passwordHash, String profileImageUrl) {
        if (memberRepository.existsByEmail(email)) {
            throw new ServiceException("409-1", "이미 사용중인 이메일입니다.");
        }

        if (memberRepository.existsByUsername(username)) {
            throw new ServiceException("409-1", "이미 사용중인 닉네임입니다.");
        }

        Member member = new Member(email, username, passwordHash, profileImageUrl, MemberStatus.ACTIVE);

        return memberRepository.save(member);
    }

    // 로그인용 조회: 없으면 이메일 존재 여부를 노출하지 않도록 비밀번호 오류와 동일한 메시지로 처리한다.
    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new ServiceException("401-1", "이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Transactional(readOnly = true)
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));
    }

    @Transactional
    public void modifyProfileImageUrl(Long id, String profileImageUrl) {
        Member member = findById(id);
        member.modifyInfo(null, null, profileImageUrl);
    }

    // 온보딩 완료(2차 저장): 소셜 가입자가 임시 닉네임을 확정하면 정상 회원으로 전환한다.
    @Transactional
    public Member completeProfile(Long memberId, String username) {
        Member member = findById(memberId);

        // 이미 온보딩을 마친 회원이 다시 호출하면 닉네임이 덮어써지므로 여기서 막는다.
        if (member.getStatus() != MemberStatus.PENDING_PROFILE) {
            throw new ServiceException(MemberErrorCode.ALREADY_ONBOARDED);
        }

        // 임시로 받은 닉네임을 그대로 확정하는 경우가 있어 본인 값은 중복으로 보지 않는다.
        if (!username.equals(member.getUsername()) && memberRepository.existsByUsername(username)) {
            throw new ServiceException("409-1", "이미 사용중인 닉네임입니다.");
        }

        member.completeProfile(username);

        return member;
    }

    // 내 정보 수정: 넘어온 필드만 반영한다. 닉네임은 실제로 바뀔 때만 중복 검사한다.
    @Transactional
    public Member modify(Long memberId, MemberModifyRequest request) {
        Member member = findById(memberId);

        if (request.username() != null
            && !request.username().equals(member.getUsername())
            && memberRepository.existsByUsername(request.username())) {
            throw new ServiceException("409-1", "이미 사용중인 닉네임입니다.");
        }

        member.modifyInfo(request.username(), request.intro(), request.profileImageUrl());

        return member;
    }

    public MemberService(final MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
