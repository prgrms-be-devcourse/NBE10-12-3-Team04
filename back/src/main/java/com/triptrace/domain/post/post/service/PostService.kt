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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Comparator

@Service
class PostService(
    private val postRepository: PostRepository,
    private val tripRepository: TripRepository,
    private val imageRepository: ImageRepository,
    private val markerRepository: MarkerRepository
) {
    @Transactional(readOnly = true)
    fun getPosts(ownerId: Long?): List<PostResponse> =
        toResponses(postRepository.findByOwnerId(ownerId).orEmpty().filterNotNull())

    @Transactional
    fun create(tripId: Long, ownerId: Long?, request: PostCreateRequest): PostResponse {
        val trip = tripRepository.findById(tripId)
            .orElseThrow { ServiceException(PostErrorCode.TRIP_NOT_FOUND) }
        validateOwner(trip, ownerId)

        val date = requireNotNull(request.date) { "게시물 날짜는 필수입니다." }
        val title = requireNotNull(request.title) { "게시물 제목은 필수입니다." }
        val post = postRepository.save(Post(trip, date, title, request.memo))

        markerRepository.save(
            Marker(
                post,
                null,
                null,
                null,
                toVisitedAt(date, request.time),
                MarkerSource.MANUAL
            )
        )
        recalculateTripDateRange(trip)

        log.info(
            "[POST] create completed postId: {}, tripId: {}, ownerId: {}",
            post.getId(),
            tripId,
            ownerId
        )

        return toResponse(post)
    }

    @Transactional(readOnly = true)
    fun findPostsByTripId(tripId: Long, ownerId: Long?): List<PostResponse> {
        val trip = tripRepository.findById(tripId)
            .orElseThrow { ServiceException(PostErrorCode.TRIP_NOT_FOUND) }

        if (!trip.isVisibility()) {
            validateOwner(trip, ownerId)
        }

        val posts = postRepository.findByTripIdOrderByDateAsc(tripId).orEmpty().filterNotNull()
        return toResponses(posts)
    }

    @Transactional(readOnly = true)
    fun findAccessiblePost(postId: Long, ownerId: Long?): PostResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { ServiceException(PostErrorCode.NOT_FOUND) }

        if (!post.trip.isVisibility()) {
            validateOwner(post.trip, ownerId)
        }

        return toResponse(post)
    }

    @Transactional
    fun modifyPost(postId: Long, ownerId: Long?, request: PostModifyRequest): PostResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { ServiceException(PostErrorCode.NOT_FOUND) }
        validateOwner(post.trip, ownerId)

        post.modify(
            requireNotNull(request.date) { "게시물 날짜는 필수입니다." },
            requireNotNull(request.title) { "게시물 제목은 필수입니다." },
            request.memo
        )
        syncMarkerDate(post)
        recalculateTripDateRange(post.trip)

        log.info(
            "[POST] modify completed postId: {}, tripId: {}, ownerId: {}",
            postId,
            post.trip.getId(),
            ownerId
        )

        return toResponse(post)
    }

    @Transactional
    fun deletePost(postId: Long, ownerId: Long?) {
        val post = postRepository.findById(postId)
            .orElseThrow { ServiceException(PostErrorCode.NOT_FOUND) }
        validateOwner(post.trip, ownerId)

        val representativeImage = post.trip.representativeImage
        val usesRepresentativeImage = representativeImage?.post?.id == postId

        markerRepository.findByPostId(postId).ifPresent(markerRepository::delete)
        imageRepository.findByPostId(postId).forEach { it.disconnectPost() }

        if (usesRepresentativeImage) {
            post.trip.changeRepresentativeImage(null)
        }

        postRepository.delete(post)
        postRepository.flush()
        recalculateTripDateRange(post.trip)

        log.info(
            "[POST] delete completed postId: {}, tripId: {}, ownerId: {}",
            postId,
            post.trip.getId(),
            ownerId
        )
    }

    private fun validateOwner(trip: Trip, ownerId: Long?) {
        if (trip.owner.getId() != ownerId) {
            throw ServiceException(PostErrorCode.FORBIDDEN)
        }
    }

    private fun toResponse(post: Post): PostResponse {
        val images = imageRepository.findByPostId(post.getId())
        val marker = markerRepository.findByPostId(post.getId()).orElse(null)
        return PostResponse(post, images, marker)
    }

    private fun toResponses(posts: List<Post>): List<PostResponse> {
        if (posts.isEmpty()) {
            return emptyList()
        }

        val postIds = posts.map { it.getId() }
        val imagesByPostId = imageRepository.findByPostIdIn(postIds).groupBy { it.post?.id }
        val markerByPostId = markerRepository.findByPostIdIn(postIds).associateBy { it.post.getId() }

        return posts
            .sortedWith(
                compareBy<Post> { it.date }
                    .thenComparator { left, right ->
                        NULLS_LAST_TIME_COMPARATOR.compare(
                            resolveTime(markerByPostId[left.getId()]),
                            resolveTime(markerByPostId[right.getId()])
                        )
                    }
                    .thenBy { it.getId() }
            )
            .map { post ->
                PostResponse(
                    post,
                    imagesByPostId[post.getId()].orEmpty(),
                    markerByPostId[post.getId()]
                )
            }
    }

    private fun resolveTime(marker: Marker?): LocalTime? = marker?.visitedAt?.toLocalTime()

    private fun toVisitedAt(date: LocalDate, time: LocalTime?): LocalDateTime? =
        time?.let { LocalDateTime.of(date, it) }

    private fun syncMarkerDate(post: Post) {
        val marker = markerRepository.findByPostId(post.getId())
            .orElseGet {
                markerRepository.save(
                    Marker(post, null, null, null, null, MarkerSource.MANUAL)
                )
            }

        val visitedAt = marker.visitedAt ?: return
        marker.modify(
            marker.centerLat,
            marker.centerLng,
            marker.placeName,
            LocalDateTime.of(post.date, visitedAt.toLocalTime()),
            marker.source
        )
    }

    private fun recalculateTripDateRange(trip: Trip) {
        val firstDate = postRepository.findFirstByTripIdOrderByDateAscIdAsc(trip.getId())
            ?.map { it?.date }
            ?.orElse(null)
        val lastDate = postRepository.findFirstByTripIdOrderByDateDescIdDesc(trip.getId())
            ?.map { it?.date }
            ?.orElse(null)

        trip.changeDateRange(firstDate?.atStartOfDay(), lastDate?.atStartOfDay())
    }

    @Transactional(readOnly = true)
    fun getPost(postId: Long): Post = postRepository.findById(postId)
        .orElseThrow { ServiceException(PostErrorCode.NOT_FOUND) }

    @Transactional(readOnly = true)
    fun getPost(trip: Trip, postId: Long): Post {
        val post = getPost(postId)
        if (post.trip.getId() != trip.getId()) {
            throw ServiceException(PostErrorCode.NOT_FOUND)
        }
        return post
    }

    companion object {
        private val log = LoggerFactory.getLogger(PostService::class.java)
        private val NULLS_LAST_TIME_COMPARATOR: Comparator<LocalTime?> =
            Comparator.nullsLast(Comparator.naturalOrder())
    }
}
