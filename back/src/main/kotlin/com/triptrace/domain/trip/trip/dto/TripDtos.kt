package com.triptrace.domain.trip.trip.dto

import com.triptrace.domain.trip.trip.entity.Trip
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

@JvmRecord data class PopularDestinationResponse(
    val country: String,
    val city: String,
    val tripCount: Long,
    val thumbnailUrl: String?,
    val representativeTripId: Long?,
)

@JvmRecord data class TripCreateRequest(
    @field:NotBlank @field:Size(max = 100) val title: String,
    @field:Size(max = 100) val country: String?,
    @field:Size(max = 100) val city: String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val visibility: Boolean,
)

@JvmRecord data class TripModifyRequest(
    @field:NotBlank @field:Size(max = 100) val title: String,
    @field:Size(max = 100) val country: String?,
    @field:Size(max = 100) val city: String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val visibility: Boolean,
)

@JvmRecord data class TripRepresentativeImageRequest(
    @field:NotNull val imageId: Long,
)

@JvmRecord data class TripResponse(
    val id: Long?,
    val ownerId: Long?,
    val author: AuthorResponse,
    val thumbnailUrl: String?,
    val representativeLat: BigDecimal?,
    val representativeLng: BigDecimal?,
    val title: String,
    val country: String?,
    val city: String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val visibility: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val likeCount: Long,
) {
    constructor(trip: Trip) : this(
        trip.id,
        trip.owner.id,
        AuthorResponse(trip.owner.id, trip.owner.username, trip.owner.profileImageUrl),
        trip.representativeImage?.thumbnailUrl,
        trip.representativeImage?.gpsLat,
        trip.representativeImage?.gpsLng,
        trip.title,
        trip.country,
        trip.city,
        trip.startDate,
        trip.endDate,
        trip.visibility,
        trip.createdAt,
        trip.updatedAt,
        trip.likeCount,
    )

    @JvmRecord data class AuthorResponse(
        val id: Long?,
        val nickname: String,
        val profileImageUrl: String?,
    )
}

@JvmRecord data class TripSearchCondition(
    val tokens: List<String>?,
    val scope: TripSearchScope,
    val country: String?,
    val city: String?,
    val sort: TripSearchSort,
) {
    fun hasKeyword(): Boolean = !tokens.isNullOrEmpty()
}

@JvmRecord data class TripSearchLocation(
    val country: String,
    val city: String,
)

@JvmRecord data class TripSearchLocationResponse(
    val country: String,
    val cities: List<String>,
)

@JvmRecord data class TripSearchResponse(
    val tripId: Long?,
    val title: String,
    val thumbnailUrl: String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val country: String?,
    val city: String?,
    val previewText: String?,
)

enum class TripSearchScope { TRIP_TITLE, POST_TITLE, POST_CONTENT, ALL }

enum class TripSearchSort { LATEST, OLDEST, MOST_LIKED, LEAST_LIKED }

@JvmRecord data class WeeklyTrendingTripResponse(
    val trip: TripResponse,
    val weeklyLikeCount: Long,
)
