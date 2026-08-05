package com.triptrace.domain.trip.tripLike.controller

import com.triptrace.domain.trip.tripLike.dto.TripLikeStatusResponse
import com.triptrace.domain.trip.tripLike.service.TripLikeService
import com.triptrace.global.app.Domain
import com.triptrace.global.rsData.RsData
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips")
class TripLikeController(
    private val tripLikeService: TripLikeService
) {

    // 좋아요 추가
    @PostMapping("/{tripId}/likes")
    fun createLike(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable tripId: Long
    ): RsData<Void> {
        tripLikeService.createLike(memberId, tripId)

        return RsData(
            CREATED_CODE,
            "좋아요가 등록되었습니다."
        )
    }

    // 좋아요 취소
    @DeleteMapping("/{tripId}/likes")
    fun deleteLike(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable tripId: Long
    ): RsData<Void> {
        tripLikeService.deleteLike(memberId, tripId)

        return RsData(
            SUCCESS_CODE,
            "좋아요가 취소되었습니다."
        )
    }

    // 좋아요 여부 조회
    @GetMapping("/{tripId}/likes/me")
    fun isLiked(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable tripId: Long
    ): RsData<TripLikeStatusResponse> {
        val liked = tripLikeService.isLiked(memberId, tripId)

        return RsData(
            SUCCESS_CODE,
            "좋아요 여부 조회 성공했습니다.",
            TripLikeStatusResponse(liked)
        )
    }

    companion object {
        private val SUCCESS_CODE = "200-" + Domain.TRIP.code
        private val CREATED_CODE = "201-" + Domain.TRIP.code
    }
}
