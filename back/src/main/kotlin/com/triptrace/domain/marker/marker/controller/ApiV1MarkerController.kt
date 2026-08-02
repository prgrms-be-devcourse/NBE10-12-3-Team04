package com.triptrace.domain.marker.marker.controller

import com.triptrace.domain.marker.marker.dto.MarkerCreateRequest
import com.triptrace.domain.marker.marker.dto.MarkerModifyRequest
import com.triptrace.domain.marker.marker.dto.MarkerResponse
import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse
import com.triptrace.domain.marker.marker.service.MarkerService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1")
class ApiV1MarkerController(
    private val markerService: MarkerService,
) {
    @PostMapping("/posts/{postId}/markers")
    fun createMarker(
        @PathVariable postId: Long,
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: MarkerCreateRequest,
    ): RsData<MarkerResponse> = RsData("201-04", "마커가 생성되었습니다.", markerService.createMarker(postId, memberId, request))

    @GetMapping("/posts/{postId}/markers")
    fun getMarkers(
        @PathVariable postId: Long,
    ): RsData<List<MarkerResponse>> = RsData("200-04", "마커 목록 조회에 성공했습니다.", markerService.getMarkers(postId))

    @GetMapping("/posts/markers/{markerId}")
    fun getMarker(
        @PathVariable markerId: Long,
    ): RsData<MarkerResponse> = RsData("200-04", "마커 조회에 성공했습니다.", markerService.getMarker(markerId))

    @GetMapping("/posts/markers/{markerId}/place-candidates")
    fun getPlaceCandidates(
        @PathVariable markerId: Long,
        @AuthenticationPrincipal memberId: Long,
    ): RsData<List<PlaceCandidateResponse>> =
        RsData("200-04", "마커 장소명 후보 조회에 성공했습니다.", markerService.getPlaceCandidates(markerId, memberId))

    @GetMapping("/places/search")
    fun searchPlaces(
        @RequestParam keyword: String,
    ): RsData<List<PlaceCandidateResponse>> = RsData("200-04", "장소 검색에 성공했습니다.", markerService.searchPlaces(keyword))

    @GetMapping("/places/nearby")
    fun findNearbyPlaces(
        @RequestParam latitude: BigDecimal,
        @RequestParam longitude: BigDecimal,
    ): RsData<List<PlaceCandidateResponse>> = RsData("200-04", "주변 장소 조회에 성공했습니다.", markerService.findNearbyPlaces(latitude, longitude))

    @PatchMapping("/posts/markers/{markerId}")
    fun modifyMarker(
        @PathVariable markerId: Long,
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: MarkerModifyRequest,
    ): RsData<MarkerResponse> = RsData("200-04", "마커가 수정되었습니다.", markerService.modifyMarker(markerId, memberId, request))

    @DeleteMapping("/posts/markers/{markerId}")
    fun deleteMarker(
        @PathVariable markerId: Long,
        @AuthenticationPrincipal memberId: Long,
    ) = markerService.deleteMarker(markerId, memberId)
}
