package com.triptrace.domain.post.post.controller

import com.triptrace.domain.post.post.dto.PostCreateRequest
import com.triptrace.domain.post.post.dto.PostModifyRequest
import com.triptrace.domain.post.post.dto.PostResponse
import com.triptrace.domain.post.post.service.PostService
import com.triptrace.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ApiV1PostController(
    private val postService: PostService,
) {
    @PostMapping("/trips/{tripId}/posts")
    fun createPost(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long,
        @RequestBody @Valid request: PostCreateRequest,
    ): RsData<PostResponse> {
        val response = postService.create(tripId, memberId, request)
        return RsData("201-06", "${response.id}번 게시물이 생성되었습니다.", response)
    }

    @GetMapping("/trips/{tripId}/posts")
    fun getPosts(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long,
    ): RsData<List<PostResponse>> = RsData("200-06", "게시물 목록 조회에 성공했습니다.", postService.findPostsByTripId(tripId, memberId))

    @GetMapping("/posts")
    fun getPosts(
        @AuthenticationPrincipal memberId: Long,
    ): RsData<List<PostResponse>> = RsData("200-06", "게시물 목록 조회에 성공했습니다.", postService.getPosts(memberId))

    @GetMapping("/posts/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @AuthenticationPrincipal memberId: Long?,
    ): RsData<PostResponse> = RsData("200-06", "${postId}번 게시물 조회에 성공했습니다.", postService.findAccessiblePost(postId, memberId))

    @PatchMapping("/posts/{postId}")
    fun modifyPost(
        @PathVariable postId: Long,
        @AuthenticationPrincipal memberId: Long,
        @RequestBody @Valid request: PostModifyRequest,
    ): RsData<PostResponse> = RsData("200-06", "${postId}번 게시물이 수정되었습니다.", postService.modifyPost(postId, memberId, request))

    @DeleteMapping("/posts/{postId}")
    fun deletePost(
        @PathVariable postId: Long,
        @AuthenticationPrincipal memberId: Long,
    ): RsData<Void> {
        postService.deletePost(postId, memberId)
        return RsData("200-06", "${postId}번 게시물이 삭제되었습니다.")
    }
}
