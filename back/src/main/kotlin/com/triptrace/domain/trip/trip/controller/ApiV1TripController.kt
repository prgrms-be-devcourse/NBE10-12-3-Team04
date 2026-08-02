package com.triptrace.domain.trip.trip.controller

import com.triptrace.domain.trip.trip.dto.TripCreateRequest
import com.triptrace.domain.trip.trip.dto.TripModifyRequest
import com.triptrace.domain.trip.trip.dto.TripRepresentativeImageRequest
import com.triptrace.domain.trip.trip.dto.TripResponse
import com.triptrace.domain.trip.trip.service.TripService
import com.triptrace.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
class ApiV1TripController(
    private val tripService: TripService,
) {
    @PostMapping("/trips")
    fun createTrip(
        @AuthenticationPrincipal memberId: Long,
        @RequestBody @Valid request: TripCreateRequest,
    ): RsData<TripResponse> {
        val response = tripService.create(memberId, request)
        return RsData("201-07", "${response.id}번 여행기가 생성되었습니다.", response)
    }

    @GetMapping("/users/me/trips")
    fun getMyTrips(
        @AuthenticationPrincipal memberId: Long,
    ): RsData<List<TripResponse>> = RsData("200-07", "내 여행기 목록 조회에 성공했습니다.", tripService.findTripsByOwnerId(memberId))

    @GetMapping(value = ["/users/me/trips"], params = ["page", "size"])
    fun getMyTrips(
        @AuthenticationPrincipal memberId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): RsData<Page<TripResponse>> = RsData("200-07", "내 여행기 목록 조회에 성공했습니다.", tripService.findTripsByOwnerId(memberId, pageable))

    @GetMapping("/trips")
    fun getTrips(): RsData<List<TripResponse>> = RsData("200-07", "공개 여행기 목록 조회에 성공했습니다.", tripService.findPublicTrips())

    @GetMapping("/trips/{tripId}")
    fun getTrip(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long?,
    ): RsData<TripResponse> = RsData("200-07", "${tripId}번 여행기 조회에 성공했습니다.", tripService.findAccessibleTrip(tripId, memberId))

    @PatchMapping("/trips/{tripId}")
    fun modifyTrip(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long,
        @RequestBody @Valid request: TripModifyRequest,
    ): RsData<TripResponse> = RsData("200-07", "${tripId}번 여행기가 수정되었습니다.", tripService.modifyTrip(tripId, memberId, request))

    @DeleteMapping("/trips/{tripId}")
    fun deleteTrip(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long,
    ): RsData<Void> {
        tripService.deleteTrip(tripId, memberId)
        return RsData("200-07", "${tripId}번 여행기가 삭제되었습니다.")
    }

    @PatchMapping("/trips/{tripId}/representative-image")
    fun changeRepresentativeImage(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long,
        @RequestBody @Valid request: TripRepresentativeImageRequest,
    ): RsData<TripResponse> =
        RsData("200-07", "${tripId}번 여행기 대표이미지가 수정되었습니다.", tripService.changeRepresentativeImage(tripId, memberId, request.imageId))
}
