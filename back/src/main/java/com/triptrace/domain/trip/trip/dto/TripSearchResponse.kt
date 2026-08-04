package com.triptrace.domain.trip.trip.dto

import java.time.LocalDateTime

// 검색 결과 DTO
@JvmRecord
data class TripSearchResponse(
    val tripId: Long?,
    val title: String?,
    val thumbnailUrl: String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val country: String?,
    val city: String?,
    val previewText: String?
)
