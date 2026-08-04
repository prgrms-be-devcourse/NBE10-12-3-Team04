package com.triptrace.domain.trip.trip.controller

import com.triptrace.domain.trip.trip.dto.*
import com.triptrace.domain.trip.trip.service.TripSearchService
import com.triptrace.domain.trip.trip.service.TripService
import com.triptrace.global.app.Domain
import com.triptrace.global.rsData.RsData
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/feed/trips")
class ApiV1TripFeedController(
    private val tripService: TripService,
    private val tripSearchService: TripSearchService
) {
    @get:GetMapping("/top-liked")
    val top10PublicTripsByLikeCount: RsData<List<TripResponse>>
        get() = RsData(
            SUCCESS_CODE,
            "좋아요 상위 10개 여행기 조회에 성공했습니다.",
            tripService.findTop10PublicTripsByLikeCount()
        )

    @get:GetMapping("/recent")
    val visibilityTrueOrderByCreatedAtDesc: RsData<List<TripResponse>>
        get() = RsData(
            SUCCESS_CODE,
            "여행기 최신순 조회에 성공했습니다.",
            tripService.findPublicTripsByCreatedAtDesc()
        )

    @get:GetMapping("/trending-weekly")
    val weeklyTrendingTrips: RsData<List<WeeklyTrendingTripResponse>>
        get() = RsData(
            SUCCESS_CODE,
            "이번 주 급상승 여행 조회에 성공했습니다.",
            tripService.findWeeklyTrendingTrips()
        )

    @get:GetMapping("/popular-destinations")
    val popularDestinations: RsData<List<PopularDestinationResponse>>
        get() = RsData(
            SUCCESS_CODE,
            "인기 여행지 조회에 성공했습니다.",
            tripService.findPopularDestinations()
        )

    @GetMapping(value = ["/recent"], params = ["page", "size"])
    fun getVisibilityTrueOrderByCreatedAtDesc(
        @PageableDefault(size = 20) pageable: Pageable
    ): RsData<Page<TripResponse>> {
        return RsData(
            SUCCESS_CODE,
            "여행기 최신순 조회에 성공했습니다.",
            tripService.findPublicTripsByCreatedAtDesc(pageable)
        )
    }

    @GetMapping("/search")
    fun searchTrips(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "ALL") scope: TripSearchScope?,
        @RequestParam(required = false) country: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(defaultValue = "LATEST") sort: TripSearchSort?,
        @PageableDefault(size = 12) pageable: Pageable
    ): RsData<Page<TripSearchResponse>> {
        return RsData(
            SUCCESS_CODE,
            "여행기 검색에 성공했습니다.",
            tripSearchService.search(
                keyword,
                scope,
                country,
                city,
                sort,
                pageable
            )
        )
    }

    @get:GetMapping("/search/locations")
    val searchLocations: RsData<List<TripSearchLocationResponse>>
        get() = RsData(
            SUCCESS_CODE,
            "여행기 검색 지역 조회에 성공했습니다.",
            tripSearchService.findLocations()
        )

    companion object {
        private val SUCCESS_CODE = "200-" + Domain.TRIP.getCode()
    }
}
