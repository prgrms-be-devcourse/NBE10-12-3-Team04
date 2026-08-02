package com.triptrace.domain.marker.marker.geocoding

@JvmRecord
data class ReverseGeocodingResult(
    val country: String?,
    val city: String?,
    val placeName: String?,
)
