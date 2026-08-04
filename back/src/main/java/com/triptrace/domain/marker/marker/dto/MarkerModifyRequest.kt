package com.triptrace.domain.marker.marker.dto

import com.triptrace.domain.marker.marker.entity.MarkerSource
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

@JvmRecord
data class MarkerModifyRequest(
    val centerLat: BigDecimal?,

    val centerLng: BigDecimal?,

    @field:Size(max = 100)
    val placeName: String?,

    val visitedAt: LocalDateTime?,

    @field:NotNull
    val source: MarkerSource?
)
