package com.triptrace.domain.post.post.controller

import com.triptrace.domain.post.post.dto.PostCreateRequest
import com.triptrace.domain.post.post.dto.PostModifyRequest
import com.triptrace.domain.post.post.dto.PostResponse
import com.triptrace.domain.post.post.service.PostService
import com.triptrace.global.app.Domain
import com.triptrace.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class ApiV1PostController(private val postService: PostService) {
    @PostMapping("/trips/{tripId}/posts")
    fun createPost(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long?,
        @RequestBody @Valid request: PostCreateRequest
    ): RsData<PostResponse> {
        val response = postService.create(tripId, memberId, request)

        return RsData(
            CREATED_CODE,
            "${response.id}번 게시물이 생성되었습니다.",
            response
        )
    }

    @GetMapping("/trips/{tripId}/posts")
    fun getPosts(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long?
    ): RsData<List<PostResponse>> {
        return RsData(
            SUCCESS_CODE,
            "게시물 목록 조회에 성공했습니다.",
            postService.findPostsByTripId(tripId, memberId)
        )
    }

    @GetMapping("/posts")
    fun getPosts(
        @AuthenticationPrincipal memberId: Long?
    ): RsData<List<PostResponse>> {
        return RsData(
            SUCCESS_CODE,
            "게시물 목록 조회에 성공했습니다.",
            postService.getPosts(memberId)
        )
    }

    @GetMapping("/posts/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @AuthenticationPrincipal memberId: Long?
    ): RsData<PostResponse> {
        return RsData(
            SUCCESS_CODE,
            "${postId}번 게시물 조회에 성공했습니다.",
            postService.findAccessiblePost(postId, memberId)
        )
    }

    @PatchMapping("/posts/{postId}")
    fun modifyPost(
        @PathVariable postId: Long,
        @AuthenticationPrincipal memberId: Long?,
        @RequestBody @Valid request: PostModifyRequest
    ): RsData<PostResponse> {
        return RsData(
            SUCCESS_CODE,
            "${postId}번 게시물이 수정되었습니다.",
            postService.modifyPost(postId, memberId, request)
        )
    }

    @DeleteMapping("/posts/{postId}")
    fun deletePost(
        @PathVariable postId: Long,
        @AuthenticationPrincipal memberId: Long?
    ): RsData<Void?> {
        postService.deletePost(postId, memberId)
        return RsData(
            SUCCESS_CODE,
            "${postId}번 게시물이 삭제되었습니다."
        )
    }

    companion object {
        private val SUCCESS_CODE = "200-" + Domain.POST.getCode()
        private val CREATED_CODE = "201-" + Domain.POST.getCode()
    }
}
