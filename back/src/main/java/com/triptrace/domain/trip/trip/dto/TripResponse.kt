package com.triptrace.domain.trip.trip.dto

import com.triptrace.domain.trip.trip.entity.Trip
import java.math.BigDecimal
import java.time.LocalDateTime

@JvmRecord
data class TripResponse(
    val id: Long?,
    val ownerId: Long?,
    val author: AuthorResponse?,
    val thumbnailUrl: String?,
    val representativeLat: BigDecimal?,
    val representativeLng: BigDecimal?,
    val title: String?,
    val country: String?,
    val city: String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val visibility: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val likeCount: Long?
) {
    constructor(trip: Trip) : this(
        trip.getId(),
        trip.owner.getId(),
        AuthorResponse(
            trip.owner.getId(),
            trip.owner.username,
            trip.owner.profileImageUrl
        ),
        trip.representativeImage?.thumbnailUrl,
        trip.representativeImage?.gpsLat,
        trip.representativeImage?.gpsLng,
        trip.title,
        trip.country,
        trip.city,
        trip.startDate,
        trip.endDate,
        trip.isVisibility(),
        trip.getCreatedAt(),
        trip.getUpdatedAt(),
        trip.likeCount
    )

    @JvmRecord
    data class AuthorResponse(
        val id: Long?,
        val nickname: String?,
        val profileImageUrl: String?
    )
}
