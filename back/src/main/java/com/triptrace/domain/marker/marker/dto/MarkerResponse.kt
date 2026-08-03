package com.triptrace.domain.marker.marker.dto

import com.triptrace.domain.marker.marker.entity.Marker
import com.triptrace.domain.marker.marker.entity.MarkerSource
import java.math.BigDecimal
import java.time.LocalDateTime

@JvmRecord
data class MarkerResponse(
    val id: Long?,
    val postId: Long?,
    val centerLat: BigDecimal?,
    val centerLng: BigDecimal?,
    val placeName: String?,
    val visitedAt: LocalDateTime?,
    val source: MarkerSource?,
    val representativeImageId: Long?,
    val representativeThumbnailUrl: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    constructor(marker: Marker) : this(
        marker.getId(),
        marker.getPost().getId(),
        marker.getCenterLat(),
        marker.getCenterLng(),
        marker.getPlaceName(),
        marker.getVisitedAt(),
        marker.getSource(),
        if (marker.getRepresentativeImage() == null) null else marker.getRepresentativeImage().getId(),
        if (marker.getRepresentativeImage() == null) null else marker.getRepresentativeImage().getThumbnailUrl(),
        marker.getCreatedAt(),
        marker.getUpdatedAt()
    )
}
