package com.triptrace.domain.marker.marker.geocoding

import java.math.BigDecimal

interface ReverseGeocodingClient {
    fun findPlaceName(
        latitude: BigDecimal,
        longitude: BigDecimal,
    ): String?

    fun findLocation(
        latitude: BigDecimal,
        longitude: BigDecimal,
    ): ReverseGeocodingResult? = ReverseGeocodingResult(null, null, findPlaceName(latitude, longitude))
}
