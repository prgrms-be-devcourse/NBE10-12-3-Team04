package com.triptrace.domain.trip.tripLike.controller;

import com.triptrace.domain.trip.tripLike.dto.TripLikeStatusResponse;
import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import com.triptrace.global.app.Domain;
import com.triptrace.global.rsData.RsData;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
public class TripLikeController {
    private static final String SUCCESS_CODE = "200-" + Domain.TRIP.getCode();
    private static final String CREATED_CODE = "201-" + Domain.TRIP.getCode();
    private final TripLikeService tripLikeService;

    // 좋아요 추가
    @PostMapping("/{tripId}/likes")
    public RsData<Void> createLike(
        @AuthenticationPrincipal Long memberId,
        @PathVariable Long tripId) {

        tripLikeService.createLike(memberId, tripId);

        return new RsData<>(
            CREATED_CODE,
            "좋아요가 등록되었습니다."
        );
    }

    // 좋아요 취소
    @DeleteMapping("/{tripId}/likes")
    public RsData<Void> deleteLike(
        @AuthenticationPrincipal Long memberId,
        @PathVariable Long tripId) {

        tripLikeService.deleteLike(memberId, tripId);

        return new RsData<>(
            SUCCESS_CODE,
            "좋아요가 취소되었습니다."
        );
    }

    // 좋아요 여부 조회
    @GetMapping("/{tripId}/likes/me")
    public RsData<TripLikeStatusResponse> isLiked(
        @AuthenticationPrincipal Long memberId,
        @PathVariable Long tripId
    ) {
        boolean liked = tripLikeService.isLiked(memberId, tripId);

        return new RsData<>(
            SUCCESS_CODE,
            "좋아요 여부 조회 성공했습니다.",
            new TripLikeStatusResponse(liked)
        );
    }

    public TripLikeController(final TripLikeService tripLikeService) {
        this.tripLikeService = tripLikeService;
    }
}
