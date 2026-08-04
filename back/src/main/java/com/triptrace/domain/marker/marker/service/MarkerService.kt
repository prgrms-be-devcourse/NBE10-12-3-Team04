package com.triptrace.domain.marker.marker.service

import com.triptrace.domain.marker.marker.dto.MarkerCreateRequest
import com.triptrace.domain.marker.marker.dto.MarkerModifyRequest
import com.triptrace.domain.marker.marker.dto.MarkerResponse
import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse
import com.triptrace.domain.marker.marker.entity.Marker
import com.triptrace.domain.marker.marker.error.MarkerErrorCode
import com.triptrace.domain.marker.marker.place.GooglePlacesClient
import com.triptrace.domain.marker.marker.repository.MarkerRepository
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.post.post.repository.PostRepository
import com.triptrace.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class MarkerService(
    private val markerRepository: MarkerRepository,
    private val postRepository: PostRepository,
    private val googlePlacesClient: GooglePlacesClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 권한 체크
    private fun validateOwner(post: Post, memberId: Long?) {
        val ownerId = post.trip.getOwner().getId()

        if (ownerId != memberId) {
            throw ServiceException(MarkerErrorCode.FORBIDDEN)
        }
    }

    // 생성
    @Transactional
    fun createMarker(postId: Long, memberId: Long?, request: MarkerCreateRequest): MarkerResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { ServiceException(MarkerErrorCode.POST_NOT_FOUND) }

        validateOwner(post, memberId)

        val marker = Marker(
            post,
            request.centerLat,
            request.centerLng,
            request.placeName,
            request.visitedAt,
            requireNotNull(request.source)
        )

        val saved = markerRepository.save(marker)

        log.info(
            "[MARKER] create completed markerId: {}, postId: {}, ownerId: {}, source: {}",
            saved.id,
            postId,
            memberId,
            saved.source
        )

        return MarkerResponse(saved)
    }

    // 목록
    fun getMarkers(postId: Long): List<MarkerResponse> {
        postRepository.findById(postId)
            .orElseThrow { ServiceException(MarkerErrorCode.POST_NOT_FOUND) }

        return markerRepository.findByPostId(postId)
            .stream()
            .map(::MarkerResponse)
            .toList()
    }

    // 상세
    fun getMarker(markerId: Long): MarkerResponse {
        val marker = markerRepository.findById(markerId)
            .orElseThrow { ServiceException(MarkerErrorCode.NOT_FOUND) }

        return MarkerResponse(marker)
    }

    // 장소명 후보 조회
    fun getPlaceCandidates(markerId: Long, memberId: Long?): List<PlaceCandidateResponse> {
        val marker = markerRepository.findById(markerId)
            .orElseThrow { ServiceException(MarkerErrorCode.NOT_FOUND) }

        validateOwner(marker.post, memberId)

        // 자동 생성 때는 지역명만 저장하고, 사용자가 수정 화면에서 펼칠 때만 주변 상호명을 조회한다.
        return googlePlacesClient.findNearbyPlaces(marker.centerLat, marker.centerLng)
    }

    fun searchPlaces(keyword: String?): List<PlaceCandidateResponse> {
        if (!StringUtils.hasText(keyword)) {
            throw ServiceException(MarkerErrorCode.KEYWORD_REQUIRED)
        }

        return googlePlacesClient.searchPlaces(keyword)
    }

    fun findNearbyPlaces(latitude: BigDecimal?, longitude: BigDecimal?): List<PlaceCandidateResponse> {
        if (latitude == null || longitude == null) {
            throw ServiceException(MarkerErrorCode.COORDINATES_REQUIRED)
        }

        return googlePlacesClient.findNearbyPlaces(latitude, longitude)
    }

    // 수정
    @Transactional
    fun modifyMarker(markerId: Long, memberId: Long?, request: MarkerModifyRequest): MarkerResponse {
        val marker = markerRepository.findById(markerId)
            .orElseThrow { ServiceException(MarkerErrorCode.NOT_FOUND) }

        validateOwner(marker.post, memberId)

        marker.modify(
            request.centerLat,
            request.centerLng,
            request.placeName,
            alignVisitedAtWithPostDate(marker.post, request.visitedAt),
            requireNotNull(request.source)
        )

        log.info(
            "[MARKER] modify completed markerId: {}, postId: {}, ownerId: {}, source: {}",
            markerId,
            marker.post.id,
            memberId,
            marker.source
        )

        return MarkerResponse(marker)
    }

    // 삭제
    fun deleteMarker(markerId: Long, memberId: Long?) {
        val marker = markerRepository.findById(markerId)
            .orElseThrow { ServiceException(MarkerErrorCode.NOT_FOUND) }

        validateOwner(marker.post, memberId)
        throw ServiceException(MarkerErrorCode.DELETE_NOT_ALLOWED)
    }

    private fun alignVisitedAtWithPostDate(post: Post, visitedAt: LocalDateTime?): LocalDateTime? {
        if (visitedAt == null) {
            return null
        }
        return LocalDateTime.of(post.date, visitedAt.toLocalTime())
    }
}
