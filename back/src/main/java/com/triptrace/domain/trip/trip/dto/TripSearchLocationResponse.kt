package com.triptrace.domain.trip.trip.dto

@JvmRecord
data class TripSearchLocationResponse(
    val country: String,
    val cities: List<String>
)
