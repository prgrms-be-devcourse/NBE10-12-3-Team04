package com.triptrace.domain.post.post.dto

import com.triptrace.domain.image.image.entity.Image
import com.triptrace.domain.marker.marker.entity.Marker
import com.triptrace.domain.post.post.entity.Post
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@JvmRecord
data class PostResponse(
    val id: Long?,
    val tripId: Long?,
    val date: LocalDate,
    val time: LocalTime?,
    val title: String,
    val memo: String?,
    val images: List<PostImageResponse>,
    val marker: PostMarkerResponse?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    constructor(post: Post) : this(post, emptyList(), null)
    constructor(post: Post, images: List<Image>, marker: Marker?) : this(
        post.id,
        post.trip.id,
        post.date,
        marker?.visitedAt?.toLocalTime(),
        post.title,
        post.memo,
        images.map(::PostImageResponse),
        marker?.let(::PostMarkerResponse),
        post.createdAt,
        post.updatedAt,
    )

    @JvmRecord
    data class PostImageResponse(
        val id: Long?,
        val originalFileUrl: String,
        val thumbnailUrl: String?,
        val mimeType: String,
        val capturedAt: LocalDateTime?,
    ) {
        constructor(image: Image) : this(image.id, image.originalFileUrl, image.thumbnailUrl, image.mimeType, image.capturedAt)
    }

    @JvmRecord
    data class PostMarkerResponse(
        val id: Long?,
        val postId: Long?,
        val centerLat: BigDecimal?,
        val centerLng: BigDecimal?,
        val placeName: String?,
        val visitedAt: LocalDateTime?,
        val source: String,
        val representativeImageId: Long?,
        val representativeThumbnailUrl: String?,
    ) {
        constructor(marker: Marker) : this(
            marker.id,
            marker.post.id,
            marker.centerLat,
            marker.centerLng,
            marker.placeName,
            marker.visitedAt,
            marker.source.name,
            marker.representativeImage?.id,
            marker.representativeImage?.thumbnailUrl,
        )
    }
}
