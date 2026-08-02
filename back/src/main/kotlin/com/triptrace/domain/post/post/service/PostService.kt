package com.triptrace.domain.post.post.service

import com.triptrace.domain.image.image.repository.ImageRepository
import com.triptrace.domain.marker.marker.entity.Marker
import com.triptrace.domain.marker.marker.entity.MarkerSource
import com.triptrace.domain.marker.marker.repository.MarkerRepository
import com.triptrace.domain.post.post.dto.PostCreateRequest
import com.triptrace.domain.post.post.dto.PostModifyRequest
import com.triptrace.domain.post.post.dto.PostResponse
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.post.post.error.PostErrorCode
import com.triptrace.domain.post.post.repository.PostRepository
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.domain.trip.trip.repository.TripRepository
import com.triptrace.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class PostService(
    private val postRepository: PostRepository,
    private val tripRepository: TripRepository,
    private val imageRepository: ImageRepository,
    private val markerRepository: MarkerRepository,
) {
    @Transactional(readOnly = true)
    fun getPosts(ownerId: Long): List<PostResponse> = toResponses(postRepository.findByOwnerId(ownerId))

    @Transactional
    fun create(
        tripId: Long,
        ownerId: Long,
        request: PostCreateRequest,
    ): PostResponse {
        val trip = tripRepository.findById(tripId).orElseThrow { ServiceException(PostErrorCode.TRIP_NOT_FOUND) }
        validateOwner(trip, ownerId)
        val post = postRepository.save(Post(trip, request.date, request.title, request.memo))
        markerRepository.save(Marker(post, null, null, null, toVisitedAt(request.date, request.time), MarkerSource.MANUAL))
        recalculateTripDateRange(trip)
        return toResponse(post)
    }

    @Transactional(readOnly = true)
    fun findPostsByTripId(
        tripId: Long,
        ownerId: Long,
    ): List<PostResponse> {
        val trip = tripRepository.findById(tripId).orElseThrow { ServiceException(PostErrorCode.TRIP_NOT_FOUND) }
        if (!trip.visibility) validateOwner(trip, ownerId)
        return toResponses(postRepository.findByTripIdOrderByDateAsc(tripId))
    }

    @Transactional(readOnly = true)
    fun findAccessiblePost(
        postId: Long,
        ownerId: Long?,
    ): PostResponse {
        val post = getPost(postId)
        if (!post.trip.visibility) validateOwner(post.trip, ownerId)
        return toResponse(post)
    }

    @Transactional
    fun modifyPost(
        postId: Long,
        ownerId: Long,
        request: PostModifyRequest,
    ): PostResponse {
        val post = getPost(postId)
        validateOwner(post.trip, ownerId)
        post.modify(request.date ?: post.date, request.title ?: post.title, request.memo)
        syncMarkerDate(post)
        recalculateTripDateRange(post.trip)
        return toResponse(post)
    }

    @Transactional
    fun deletePost(
        postId: Long,
        ownerId: Long,
    ) {
        val post = getPost(postId)
        validateOwner(post.trip, ownerId)
        val representativeImage = post.trip.representativeImage
        val usesRepresentativeImage = representativeImage?.post?.id == postId
        markerRepository.findByPostId(postId).ifPresent(markerRepository::delete)
        imageRepository.findByPostId(postId).forEach { it.disconnectPost() }
        if (usesRepresentativeImage) post.trip.changeRepresentativeImage(null)
        postRepository.delete(post)
        postRepository.flush()
        recalculateTripDateRange(post.trip)
    }

    private fun validateOwner(
        trip: Trip,
        ownerId: Long?,
    ) {
        if (trip.owner.id != ownerId) throw ServiceException(PostErrorCode.FORBIDDEN)
    }

    private fun toResponse(post: Post): PostResponse =
        PostResponse(post, imageRepository.findByPostId(post.id), markerRepository.findByPostId(post.id).orElse(null))

    private fun toResponses(posts: List<Post>): List<PostResponse> {
        if (posts.isEmpty()) return emptyList()
        val postIds = posts.mapNotNull { it.id }
        val imagesByPostId = imageRepository.findByPostIdIn(postIds).groupBy { it.post.id }
        val markerByPostId = markerRepository.findByPostIdIn(postIds).associateBy { it.post.id }
        return posts
            .sortedWith { left, right ->
                val dateComparison = left.date.compareTo(right.date)
                if (dateComparison != 0) {
                    dateComparison
                } else {
                    val leftTime = resolveTime(left, markerByPostId[left.id])
                    val rightTime = resolveTime(right, markerByPostId[right.id])
                    val timeComparison =
                        when {
                            leftTime == null && rightTime == null -> 0
                            leftTime == null -> 1
                            rightTime == null -> -1
                            else -> leftTime.compareTo(rightTime)
                        }
                    if (timeComparison != 0) timeComparison else compareValues(left.id, right.id)
                }
            }.map { PostResponse(it, imagesByPostId[it.id].orEmpty(), markerByPostId[it.id]) }
    }

    private fun resolveTime(
        post: Post,
        marker: Marker?,
    ): LocalTime? = marker?.visitedAt?.toLocalTime()

    private fun toVisitedAt(
        date: LocalDate?,
        time: LocalTime?,
    ): LocalDateTime? =
        if (date == null ||
            time == null
        ) {
            null
        } else {
            LocalDateTime.of(date, time)
        }

    private fun syncMarkerDate(post: Post) {
        val marker =
            markerRepository.findByPostId(post.id).orElseGet {
                markerRepository.save(Marker(post, null, null, null, null, MarkerSource.MANUAL))
            }
        val time = marker.visitedAt?.toLocalTime() ?: return
        marker.modify(marker.centerLat, marker.centerLng, marker.placeName, LocalDateTime.of(post.date, time), marker.source)
    }

    private fun recalculateTripDateRange(trip: Trip) {
        val firstDate = postRepository.findFirstByTripIdOrderByDateAscIdAsc(trip.id).map(Post::date).orElse(null)
        val lastDate = postRepository.findFirstByTripIdOrderByDateDescIdDesc(trip.id).map(Post::date).orElse(null)
        trip.changeDateRange(firstDate?.atStartOfDay(), lastDate?.atStartOfDay())
    }

    @Transactional(readOnly = true)
    fun getPost(postId: Long): Post = postRepository.findById(postId).orElseThrow { ServiceException(PostErrorCode.NOT_FOUND) }

    @Transactional(readOnly = true)
    fun getPost(
        trip: Trip,
        postId: Long,
    ): Post =
        getPost(postId).also {
            if (it.trip.id != trip.id) throw ServiceException(PostErrorCode.NOT_FOUND)
        }
}
