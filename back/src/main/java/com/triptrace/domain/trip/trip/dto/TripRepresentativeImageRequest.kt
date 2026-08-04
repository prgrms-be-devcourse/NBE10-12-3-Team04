package com.triptrace.domain.trip.trip.dto

import jakarta.validation.constraints.NotNull

@JvmRecord
data class TripRepresentativeImageRequest(
    val imageId: @NotNull Long?
)
