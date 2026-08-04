package com.triptrace.domain.trip.trip.dto

@JvmRecord
data class WeeklyTrendingTripResponse(
    val trip: TripResponse?,
    val weeklyLikeCount: Long
)
