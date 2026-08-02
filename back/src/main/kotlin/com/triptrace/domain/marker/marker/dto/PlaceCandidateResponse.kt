package com.triptrace.domain.marker.marker.dto

import java.math.BigDecimal

@JvmRecord
data class PlaceCandidateResponse(
    val placeId: String?,
    val name: String?,
    val address: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val types: List<String>,
)
