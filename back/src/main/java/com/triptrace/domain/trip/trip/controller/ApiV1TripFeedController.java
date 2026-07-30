package com.triptrace.domain.trip.trip.controller;

import com.triptrace.domain.trip.trip.dto.TripResponse;
import com.triptrace.domain.trip.trip.dto.TripSearchResponse;
import com.triptrace.domain.trip.trip.dto.TripSearchScope;
import com.triptrace.domain.trip.trip.service.TripSearchService;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.rsData.RsData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/feed/trips")
public class ApiV1TripFeedController {
    private final TripService tripService;
    private final TripSearchService tripSearchService;

    @GetMapping("/top-liked")
    public RsData<List<TripResponse>> getTop10PublicTripsByLikeCount() {
        return new RsData<> (
            "200-07",
            "좋아요 상위 10개 여행기 조회에 성공했습니다.",
            tripService.findTop10PublicTripsByLikeCount()
        );
    }

    @GetMapping("/recent")
    public RsData<List<TripResponse>> getVisibilityTrueOrderByCreatedAtDesc() {
        return new RsData<> (
            "200-07",
            "여행기 최신순 조회에 성공했습니다.",
            tripService.findPublicTripsByCreatedAtDesc()
        );
    }

    @GetMapping(value = "/recent", params = {"page", "size"})
    public RsData<Page<TripResponse>> getVisibilityTrueOrderByCreatedAtDesc(
        @PageableDefault(size = 20) Pageable pageable) {
        return new RsData<>(
            "200-07",
            "여행기 최신순 조회에 성공했습니다.",
            tripService.findPublicTripsByCreatedAtDesc(pageable));
    }

    @GetMapping("/search")
    public RsData<Page<TripSearchResponse>> searchTrips(
        @RequestParam(required = false) final String keyword,
        @RequestParam(defaultValue = "ALL") final TripSearchScope scope,
        @RequestParam(required = false) final String country,
        @RequestParam(required = false) final String city,
        @PageableDefault(size = 20) final Pageable pageable
    ) {
        return new RsData<>(
            "200-08",
            "여행기 검색에 성공했습니다.",
            tripSearchService.search(
                keyword,
                scope,
                country,
                city,
                pageable
            )
        );
    }

    public ApiV1TripFeedController(
        final TripService tripService,
        final TripSearchService tripSearchService
    ) {
        this.tripService = tripService;
        this.tripSearchService = tripSearchService;
    }
}
