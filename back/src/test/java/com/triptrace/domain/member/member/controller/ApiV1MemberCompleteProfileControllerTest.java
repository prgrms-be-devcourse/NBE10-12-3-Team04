package com.triptrace.domain.member.member.controller;

import com.triptrace.domain.member.member.entity.LoginType;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1MemberCompleteProfileControllerTest {

    private static final String PATH = "/api/v1/users/me/profile";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("온보딩 완료 API - 성공 시 200 + 확정된 username과 ACTIVE 상태")
    void completeProfile() throws Exception {
        Member member = savePendingMember("pending@gmail.com", "temp1111", "ctrl-sub-1");

        mvc.perform(patch(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "확정닉네임"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.username").value("확정닉네임"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("온보딩 완료 API - 인증 없이 호출하면 접근이 거부된다.")
    void completeProfileWithoutToken() throws Exception {
        mvc.perform(patch(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "확정닉네임"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("온보딩 완료 API - username이 비어 있으면 400")
    void completeProfileBlankUsername() throws Exception {
        Member member = savePendingMember("blank@gmail.com", "temp2222", "ctrl-sub-2");

        mvc.perform(patch(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400"));
    }

    @Test
    @DisplayName("온보딩 완료 API - 이미 ACTIVE인 회원이면 409")
    void completeProfileAlreadyActive() throws Exception {
        Member member = memberRepository.save(
            new Member("active@test.com", "activeuser", "hashed", null, MemberStatus.ACTIVE)
        );

        mvc.perform(patch(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "새닉네임"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.resultCode").value("409-05"));
    }

    @Test
    @DisplayName("온보딩 완료 API - 이미 쓰이는 username이면 409")
    void completeProfileDuplicateUsername() throws Exception {
        memberRepository.save(new Member("owner@test.com", "선점닉네임", "hashed", null, MemberStatus.ACTIVE));
        Member member = savePendingMember("dup@gmail.com", "temp3333", "ctrl-sub-3");

        mvc.perform(patch(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "선점닉네임"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    private Member savePendingMember(String email, String username, String providerId) {
        return memberRepository.save(
            Member.ofOAuth(email, LoginType.GOOGLE, providerId, username, null)
        );
    }

    private String bearer(Member member) {
        return "Bearer " + jwtProvider.generateAccessToken(member.getId(), member.getEmail());
    }
}
