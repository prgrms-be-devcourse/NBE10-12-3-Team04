package com.triptrace.domain.trip.trip.service

import com.triptrace.domain.image.image.repository.ImageRepository
import com.triptrace.domain.marker.marker.repository.MarkerRepository
import com.triptrace.domain.member.member.repository.MemberRepository
import com.triptrace.domain.post.post.repository.PostRepository
import com.triptrace.domain.trip.trip.dto.PopularDestinationResponse
import com.triptrace.domain.trip.trip.dto.TripCreateRequest
import com.triptrace.domain.trip.trip.dto.TripModifyRequest
import com.triptrace.domain.trip.trip.dto.TripResponse
import com.triptrace.domain.trip.trip.dto.WeeklyTrendingTripResponse
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.domain.trip.trip.error.TripErrorCode
import com.triptrace.domain.trip.trip.repository.TripRepository
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository
import com.triptrace.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TripService(
    private val tripRepository: TripRepository,
    private val memberRepository: MemberRepository,
    private val imageRepository: ImageRepository,
    private val postRepository: PostRepository,
    private val markerRepository: MarkerRepository,
    private val tripLikeRepository: TripLikeRepository
) {
    @Transactional
    fun create(ownerId: Long, request: TripCreateRequest): TripResponse {
        val owner = memberRepository.findById(ownerId)
            .orElseThrow { ServiceException(TripErrorCode.MEMBER_NOT_FOUND) }
        val trip = tripRepository.save(
            Trip(
                owner,
                requireNotNull(request.title),
                request.country,
                request.city,
                request.startDate,
                request.endDate,
                request.visibility
            )
        )

        log.info(
            "[TRIP] create completed tripId: {}, ownerId: {}, visibility: {}",
            trip.getId(), ownerId, trip.isVisibility()
        )
        return toResponse(trip)
    }

    @Transactional(readOnly = true)
    fun findTripsByOwnerId(ownerId: Long): List<TripResponse> =
        tripRepository.findByOwnerId(ownerId).map(::toResponse)

    @Transactional(readOnly = true)
    fun findTripsByOwnerId(ownerId: Long, pageable: Pageable): Page<TripResponse> =
        tripRepository.findByOwnerIdOrderByCreatedAtDescIdDesc(ownerId, pageable).map(::toResponse)

    @Transactional(readOnly = true)
    fun findPublicTrips(): List<TripResponse> =
        tripRepository.findByVisibilityTrue().map(::toResponse)

    @Transactional(readOnly = true)
    fun findAccessibleTrip(tripId: Long, ownerId: Long?): TripResponse {
        val trip = tripRepository.findById(tripId)
            .orElseThrow { ServiceException(TripErrorCode.NOT_FOUND) }
        if (!trip.isVisibility()) {
            validateOwner(trip, ownerId)
        }
        return toResponse(trip)
    }

    @Transactional(readOnly = true)
    fun findOwnedTrip(tripId: Long, ownerId: Long?): Trip {
        val trip = tripRepository.findById(tripId)
            .orElseThrow { ServiceException(TripErrorCode.NOT_FOUND) }
        validateOwner(trip, ownerId)
        return trip
    }

    @Transactional
    fun modifyTrip(tripId: Long, ownerId: Long?, request: TripModifyRequest): TripResponse {
        val trip = findOwnedTrip(tripId, ownerId)
        trip.modify(
            requireNotNull(request.title),
            request.country,
            request.city,
            request.startDate,
            request.endDate,
            request.visibility
        )

        log.info(
            "[TRIP] modify completed tripId: {}, ownerId: {}, visibility: {}",
            tripId, ownerId, trip.isVisibility()
        )
        return toResponse(trip)
    }

    @Transactional
    fun deleteTrip(tripId: Long, ownerId: Long?) {
        val trip = findOwnedTrip(tripId, ownerId)
        val posts = postRepository.findByTripId(tripId)
        val postIds = posts.map { it.getId() }

        trip.changeRepresentativeImage(null)
        if (postIds.isNotEmpty()) {
            markerRepository.deleteAll(markerRepository.findByPostIdIn(postIds))
        }

        tripLikeRepository.deleteByTripId(tripId)
        imageRepository.deleteAll(imageRepository.findByTripId(tripId))
        postRepository.deleteAll(posts)
        tripRepository.delete(trip)

        log.info(
            "[TRIP] delete completed tripId: {}, ownerId: {}, postCount: {}",
            tripId, ownerId, posts.size
        )
    }

    @Transactional
    fun changeRepresentativeImage(tripId: Long, ownerId: Long?, imageId: Long): TripResponse {
        val trip = findOwnedTrip(tripId, ownerId)
        val image = imageRepository.findById(imageId)
            .orElseThrow { ServiceException(TripErrorCode.IMAGE_NOT_FOUND) }

        if (image.trip.id != trip.id || image.owner.id != ownerId) {
            throw ServiceException(TripErrorCode.IMAGE_FORBIDDEN)
        }
        trip.changeRepresentativeImage(image)
        log.info(
            "[TRIP] representative image changed tripId: {}, ownerId: {}, imageId: {}",
            tripId, ownerId, imageId
        )
        return toResponse(trip)
    }

    @Transactional(readOnly = true)
    fun findTop10PublicTripsByLikeCount(): List<TripResponse> {
        val likedSince = LocalDateTime.now().minusMonths(1)
        return tripRepository.findTop10PublicTripsByRecentLikeCount(likedSince).map(::toResponse)
    }

    @Transactional(readOnly = true)
    fun findWeeklyTrendingTrips(): List<WeeklyTrendingTripResponse> {
        val likedSince = LocalDateTime.now().minusDays(7)
        return tripRepository.findWeeklyTrendingTrips(likedSince, PageRequest.of(0, 9)).map { row ->
            WeeklyTrendingTripResponse(toResponse(row[0] as Trip), (row[1] as Number).toLong())
        }
    }

    @Transactional(readOnly = true)
    fun findPopularDestinations(): List<PopularDestinationResponse> =
        tripRepository.findPopularDestinations(PageRequest.of(0, 6)).map { row ->
            val country = (row[0] as String).trim()
            val city = (row[1] as String).trim()
            val representative = tripRepository
                .findFirstByVisibilityTrueAndCountryAndCityOrderByLikeCountDescCreatedAtDescIdDesc(
                    row[0] as String,
                    row[1] as String
                )
                .orElse(null)
            PopularDestinationResponse(
                country,
                city,
                (row[2] as Number).toLong(),
                representative?.representativeImage?.thumbnailUrl,
                representative?.getId()
            )
        }

    @Transactional(readOnly = true)
    fun findPublicTripsByCreatedAtDesc(): List<TripResponse> =
        tripRepository.findByVisibilityTrueOrderByCreatedAtDesc().map(::toResponse)

    @Transactional(readOnly = true)
    fun findPublicTripsByCreatedAtDesc(pageable: Pageable): Page<TripResponse> =
        tripRepository.findByVisibilityTrueOrderByCreatedAtDescIdDesc(pageable).map(::toResponse)

    private fun toResponse(trip: Trip): TripResponse = TripResponse(trip)

    private fun validateOwner(trip: Trip, ownerId: Long?) {
        if (trip.owner.getId() != ownerId) {
            throw ServiceException(TripErrorCode.FORBIDDEN)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TripService::class.java)
    }
}
