package com.triptrace.domain.marker.marker.controller

import com.triptrace.domain.marker.marker.dto.MarkerCreateRequest
import com.triptrace.domain.marker.marker.dto.MarkerModifyRequest
import com.triptrace.domain.marker.marker.dto.MarkerResponse
import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse
import com.triptrace.domain.marker.marker.service.MarkerService
import com.triptrace.global.app.Domain
import com.triptrace.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1")
class ApiV1MarkerController(private val markerService: MarkerService) {
    private companion object {
        val SUCCESS_CODE = "200-${Domain.MARKER.code}"
        val CREATED_CODE = "201-${Domain.MARKER.code}"
    }

    // 마커 생성
    @PostMapping("/posts/{postId}/markers")
    fun createMarker(
        @PathVariable postId: Long,
        @AuthenticationPrincipal memberId: Long?,
        @Valid @RequestBody request: MarkerCreateRequest
    ): RsData<MarkerResponse> {
        val response = markerService.createMarker(postId, memberId, request)

        return RsData(
            CREATED_CODE,
            "마커가 생성되었습니다.",
            response
        )
    }

    // 마커 목록 조회
    @GetMapping("/posts/{postId}/markers")
    fun getMarkers(
        @PathVariable postId: Long
    ): RsData<List<MarkerResponse>> {
        return RsData(
            SUCCESS_CODE,
            "마커 목록 조회에 성공했습니다.",
            markerService.getMarkers(postId)
        )
    }

    // 마커 상세 조회
    @GetMapping("/posts/markers/{markerId}")
    fun getMarker(
        @PathVariable markerId: Long
    ): RsData<MarkerResponse> {
        return RsData(
            SUCCESS_CODE,
            "마커 조회에 성공했습니다.",
            markerService.getMarker(markerId)
        )
    }

    // 마커 장소명 후보 조회
    @GetMapping("/posts/markers/{markerId}/place-candidates")
    fun getPlaceCandidates(
        @PathVariable markerId: Long,
        @AuthenticationPrincipal memberId: Long?
    ): RsData<List<PlaceCandidateResponse>> {
        return RsData(
            SUCCESS_CODE,
            "마커 장소명 후보 조회에 성공했습니다.",
            markerService.getPlaceCandidates(markerId, memberId)
        )
    }

    // 장소 검색
    @GetMapping("/places/search")
    fun searchPlaces(
        @RequestParam keyword: String?
    ): RsData<List<PlaceCandidateResponse>> {
        return RsData(
            SUCCESS_CODE,
            "장소 검색에 성공했습니다.",
            markerService.searchPlaces(keyword)
        )
    }

    // 좌표 기준 주변 장소 조회
    @GetMapping("/places/nearby")
    fun findNearbyPlaces(
        @RequestParam latitude: BigDecimal,
        @RequestParam longitude: BigDecimal
    ): RsData<List<PlaceCandidateResponse>> {
        return RsData(
            SUCCESS_CODE,
            "주변 장소 조회에 성공했습니다.",
            markerService.findNearbyPlaces(latitude, longitude)
        )
    }

    // 마커 수정
    @PatchMapping("/posts/markers/{markerId}")
    fun modifyMarker(
        @PathVariable markerId: Long,
        @AuthenticationPrincipal memberId: Long?,
        @Valid @RequestBody request: MarkerModifyRequest
    ): RsData<MarkerResponse> {
        return RsData(
            SUCCESS_CODE,
            "마커가 수정되었습니다.",
            markerService.modifyMarker(markerId, memberId, request)
        )
    }

    @DeleteMapping("/posts/markers/{markerId}")
    fun deleteMarker(
        @PathVariable markerId: Long,
        @AuthenticationPrincipal memberId: Long?
    ): RsData<Void> {
        markerService.deleteMarker(markerId, memberId)
        return RsData(
            SUCCESS_CODE,
            "마커가 삭제되었습니다."
        )
    }
}
