package com.triptrace.domain.trip.trip.dto

@JvmRecord
data class PopularDestinationResponse(
    val country: String?,
    val city: String?,
    val tripCount: Long,
    val thumbnailUrl: String?,
    val representativeTripId: Long?
)
