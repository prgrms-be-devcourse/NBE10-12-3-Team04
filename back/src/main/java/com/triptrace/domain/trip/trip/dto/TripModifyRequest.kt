package com.triptrace.domain.trip.trip.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JvmRecord
data class TripModifyRequest(
    val title: @NotBlank @Size(max = 100) String?,
    val country: @Size(max = 100) String?,
    val city: @Size(max = 100) String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val visibility: Boolean
)
