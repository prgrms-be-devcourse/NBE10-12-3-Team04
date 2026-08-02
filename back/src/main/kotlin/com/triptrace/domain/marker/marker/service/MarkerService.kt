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
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class MarkerService(
    private val markerRepository: MarkerRepository,
    private val postRepository: PostRepository,
    private val googlePlacesClient: GooglePlacesClient,
) {
    private fun validateOwner(
        post: Post,
        memberId: Long,
    ) {
        if (post.trip.owner.id != memberId) throw ServiceException(MarkerErrorCode.FORBIDDEN)
    }

    @Transactional
    fun createMarker(
        postId: Long,
        memberId: Long,
        request: MarkerCreateRequest,
    ): MarkerResponse {
        val post = postRepository.findById(postId).orElseThrow { ServiceException(MarkerErrorCode.POST_NOT_FOUND) }
        validateOwner(post, memberId)
        return MarkerResponse(
            markerRepository.save(Marker(post, request.centerLat, request.centerLng, request.placeName, request.visitedAt, request.source)),
        )
    }

    fun getMarkers(postId: Long): List<MarkerResponse> {
        postRepository.findById(postId).orElseThrow { ServiceException(MarkerErrorCode.POST_NOT_FOUND) }
        return markerRepository
            .findByPostId(postId)
            .stream()
            .map(::MarkerResponse)
            .toList()
    }

    fun getMarker(markerId: Long): MarkerResponse = MarkerResponse(findMarker(markerId))

    fun getPlaceCandidates(
        markerId: Long,
        memberId: Long,
    ): List<PlaceCandidateResponse> {
        val marker = findMarker(markerId)
        validateOwner(marker.post, memberId)
        return googlePlacesClient.findNearbyPlaces(marker.centerLat, marker.centerLng)
    }

    fun searchPlaces(keyword: String?): List<PlaceCandidateResponse> {
        if (!StringUtils.hasText(keyword)) throw ServiceException(MarkerErrorCode.KEYWORD_REQUIRED)
        return googlePlacesClient.searchPlaces(keyword!!)
    }

    fun findNearbyPlaces(
        latitude: BigDecimal?,
        longitude: BigDecimal?,
    ): List<PlaceCandidateResponse> {
        if (latitude == null || longitude == null) throw ServiceException(MarkerErrorCode.COORDINATES_REQUIRED)
        return googlePlacesClient.findNearbyPlaces(latitude, longitude)
    }

    @Transactional
    fun modifyMarker(
        markerId: Long,
        memberId: Long,
        request: MarkerModifyRequest,
    ): MarkerResponse {
        val marker = findMarker(markerId)
        validateOwner(marker.post, memberId)
        marker.modify(
            request.centerLat,
            request.centerLng,
            request.placeName,
            alignVisitedAtWithPostDate(marker.post, request.visitedAt),
            request.source,
        )
        return MarkerResponse(marker)
    }

    fun deleteMarker(
        markerId: Long,
        memberId: Long,
    ) {
        val marker = findMarker(markerId)
        validateOwner(marker.post, memberId)
        throw ServiceException(MarkerErrorCode.DELETE_NOT_ALLOWED)
    }

    private fun findMarker(markerId: Long): Marker =
        markerRepository.findById(markerId).orElseThrow {
            ServiceException(MarkerErrorCode.NOT_FOUND)
        }

    private fun alignVisitedAtWithPostDate(
        post: Post,
        visitedAt: LocalDateTime?,
    ): LocalDateTime? = visitedAt?.let { LocalDateTime.of(post.date, it.toLocalTime()) }
}
