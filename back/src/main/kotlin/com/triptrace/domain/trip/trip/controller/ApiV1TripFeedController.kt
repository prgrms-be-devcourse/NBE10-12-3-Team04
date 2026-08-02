package com.triptrace.domain.trip.trip.controller

import com.triptrace.domain.trip.trip.dto.PopularDestinationResponse
import com.triptrace.domain.trip.trip.dto.TripResponse
import com.triptrace.domain.trip.trip.dto.TripSearchLocationResponse
import com.triptrace.domain.trip.trip.dto.TripSearchResponse
import com.triptrace.domain.trip.trip.dto.TripSearchScope
import com.triptrace.domain.trip.trip.dto.TripSearchSort
import com.triptrace.domain.trip.trip.dto.WeeklyTrendingTripResponse
import com.triptrace.domain.trip.trip.service.TripSearchService
import com.triptrace.domain.trip.trip.service.TripService
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
    private val tripSearchService: TripSearchService,
) {
    @GetMapping("/top-liked")
    fun getTop10PublicTripsByLikeCount(): RsData<List<TripResponse>> =
        RsData("200-07", "좋아요 상위 10개 여행기 조회에 성공했습니다.", tripService.findTop10PublicTripsByLikeCount())

    @GetMapping("/recent")
    fun getVisibilityTrueOrderByCreatedAtDesc(): RsData<List<TripResponse>> =
        RsData("200-07", "여행기 최신순 조회에 성공했습니다.", tripService.findPublicTripsByCreatedAtDesc())

    @GetMapping("/trending-weekly")
    fun getWeeklyTrendingTrips(): RsData<List<WeeklyTrendingTripResponse>> =
        RsData("200-10", "이번 주 급상승 여행 조회에 성공했습니다.", tripService.findWeeklyTrendingTrips())

    @GetMapping("/popular-destinations")
    fun getPopularDestinations(): RsData<List<PopularDestinationResponse>> =
        RsData("200-11", "인기 여행지 조회에 성공했습니다.", tripService.findPopularDestinations())

    @GetMapping(value = ["/recent"], params = ["page", "size"])
    fun getVisibilityTrueOrderByCreatedAtDesc(
        @PageableDefault(size = 20) pageable: Pageable,
    ): RsData<Page<TripResponse>> = RsData("200-07", "여행기 최신순 조회에 성공했습니다.", tripService.findPublicTripsByCreatedAtDesc(pageable))

    @GetMapping("/search")
    fun searchTrips(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "ALL") scope: TripSearchScope,
        @RequestParam(required = false) country: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(defaultValue = "LATEST") sort: TripSearchSort,
        @PageableDefault(size = 12) pageable: Pageable,
    ): RsData<Page<TripSearchResponse>> =
        RsData("200-08", "여행기 검색에 성공했습니다.", tripSearchService.search(keyword, scope, country, city, sort, pageable))

    @GetMapping("/search/locations")
    fun getSearchLocations(): RsData<List<TripSearchLocationResponse>> =
        RsData("200-09", "여행기 검색 지역 조회에 성공했습니다.", tripSearchService.findLocations())
}
