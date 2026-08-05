package com.triptrace.domain.member.member.controller

import com.triptrace.domain.member.member.dto.CompleteProfileRequest
import com.triptrace.domain.member.member.dto.MemberMeResponse
import com.triptrace.domain.member.member.dto.MemberModifyRequest
import com.triptrace.domain.member.member.dto.ProfileImageUploadResponse
import com.triptrace.domain.member.member.service.MemberService
import com.triptrace.domain.member.member.service.ProfileImageStorage
import com.triptrace.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1")
class ApiV1MemberController(
    private val memberService: MemberService,
    private val profileImageStorage: ProfileImageStorage
) {

    @GetMapping("/users/me")
    fun getMe(@AuthenticationPrincipal memberId: Long): RsData<MemberMeResponse> {
        return RsData(
            "200-1",
            "내 회원 정보 조회에 성공했습니다.",
            MemberMeResponse(memberService.findById(memberId))
        )
    }

    @PatchMapping("/users/me")
    fun modifyMe(
        @AuthenticationPrincipal memberId: Long,
        @RequestBody @Valid request: MemberModifyRequest
    ): RsData<MemberMeResponse> {
        val member = memberService.modify(memberId, request)

        return RsData(
            "200-1",
            "회원 정보가 수정되었습니다.",
            MemberMeResponse(member)
        )
    }

    // 소셜 가입자 온보딩 완료. 인증이 필요한 경로라 SecurityConfig의 anyRequest().authenticated()가 적용된다.
    @PatchMapping("/users/me/profile")
    fun completeProfile(
        @AuthenticationPrincipal memberId: Long,
        @RequestBody @Valid request: CompleteProfileRequest
    ): RsData<MemberMeResponse> {
        val member = memberService.completeProfile(memberId, request.username)

        return RsData(
            "200-1",
            "프로필 설정이 완료되었습니다.",
            MemberMeResponse(member)
        )
    }

    @PostMapping("/profile-images")
    fun uploadProfileImage(
        @RequestParam("image") image: MultipartFile
    ): RsData<ProfileImageUploadResponse> {
        val profileImageUrl = profileImageStorage.store(image)

        return RsData(
            "201-1",
            "프로필 이미지 업로드에 성공했습니다.",
            ProfileImageUploadResponse(profileImageUrl)
        )
    }
}
