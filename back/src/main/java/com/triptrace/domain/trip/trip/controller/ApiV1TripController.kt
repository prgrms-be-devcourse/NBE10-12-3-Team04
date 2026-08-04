package com.triptrace.domain.trip.trip.controller

import com.triptrace.domain.trip.trip.dto.TripCreateRequest
import com.triptrace.domain.trip.trip.dto.TripModifyRequest
import com.triptrace.domain.trip.trip.dto.TripRepresentativeImageRequest
import com.triptrace.domain.trip.trip.dto.TripResponse
import com.triptrace.domain.trip.trip.service.TripService
import com.triptrace.global.app.Domain
import com.triptrace.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class ApiV1TripController(private val tripService: TripService) {
    @PostMapping("/trips")
    fun createTrip(
        @AuthenticationPrincipal memberId: Long,
        @RequestBody @Valid request: TripCreateRequest
    ): RsData<TripResponse> {
        val response = tripService.create(memberId, request)

        return RsData(
            CREATED_CODE,
            "${response.id}번 여행기가 생성되었습니다.",
            response
        )
    }

    @GetMapping("/users/me/trips")
    fun getMyTrips(
        @AuthenticationPrincipal memberId: Long
    ): RsData<List<TripResponse>> {
        return RsData(
            SUCCESS_CODE,
            "내 여행기 목록 조회에 성공했습니다.",
            tripService.findTripsByOwnerId(memberId)
        )
    }

    @GetMapping(value = ["/users/me/trips"], params = ["page", "size"])
    fun getMyTrips(
        @AuthenticationPrincipal memberId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): RsData<Page<TripResponse>> {
        return RsData(
            SUCCESS_CODE,
            "내 여행기 목록 조회에 성공했습니다.",
            tripService.findTripsByOwnerId(memberId, pageable)
        )
    }

    @get:GetMapping("/trips")
    val trips: RsData<List<TripResponse>>
        get() = RsData(
            SUCCESS_CODE,
            "공개 여행기 목록 조회에 성공했습니다.",
            tripService.findPublicTrips()
        )

    @GetMapping("/trips/{tripId}")
    fun getTrip(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long?
    ): RsData<TripResponse> {
        return RsData(
            SUCCESS_CODE,
            "${tripId}번 여행기 조회에 성공했습니다.",
            tripService.findAccessibleTrip(tripId, memberId)
        )
    }

    @PatchMapping("/trips/{tripId}")
    fun modifyTrip(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long?,
        @RequestBody @Valid request: TripModifyRequest
    ): RsData<TripResponse> {
        return RsData(
            SUCCESS_CODE,
            "${tripId}번 여행기가 수정되었습니다.",
            tripService.modifyTrip(tripId, memberId, request)
        )
    }

    @DeleteMapping("/trips/{tripId}")
    fun deleteTrip(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long?
    ): RsData<Void?> {
        tripService.deleteTrip(tripId, memberId)

        return RsData<Void?>(
            SUCCESS_CODE,
            "${tripId}번 여행기가 삭제되었습니다."
        )
    }

    @PatchMapping("/trips/{tripId}/representative-image")
    fun changeRepresentativeImage(
        @PathVariable tripId: Long,
        @AuthenticationPrincipal memberId: Long?,
        @RequestBody @Valid request: TripRepresentativeImageRequest
    ): RsData<TripResponse> {
        return RsData(
            SUCCESS_CODE,
            "${tripId}번 여행기 대표이미지가 수정되었습니다.",
            tripService.changeRepresentativeImage(
                tripId,
                memberId,
                requireNotNull(request.imageId)
            )
        )
    }

    companion object {
        private val SUCCESS_CODE = "200-" + Domain.TRIP.getCode()
        private val CREATED_CODE = "201-" + Domain.TRIP.getCode()
    }
}
