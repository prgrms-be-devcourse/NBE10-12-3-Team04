package com.triptrace.domain.marker.marker.geocoding

import java.math.BigDecimal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import org.springframework.util.StringUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.JsonNode

@Component
class GoogleReverseGeocodingClient(
    private val apiKey: String,
    private val restClient: RestClient
) : ReverseGeocodingClient {
    private val log = LoggerFactory.getLogger(javaClass)


    @Autowired
    constructor(
        @Value("\${custom.google.maps.api-key:}") apiKey: String
    ) : this(
        apiKey,
        RestClient.builder()
            .baseUrl("https://maps.googleapis.com")
            .build()
    )

    override fun findPlaceName(latitude: BigDecimal?, longitude: BigDecimal?): String? =
        findLocation(latitude, longitude)?.placeName

    override fun findLocation(
        latitude: BigDecimal?,
        longitude: BigDecimal?
    ): ReverseGeocodingResult? {
        if (!StringUtils.hasText(apiKey) || latitude == null || longitude == null) {
            return null
        }

        return try {
            val response = restClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/maps/api/geocode/json")
                        .queryParam("latlng", "$latitude,$longitude")
                        .queryParam("language", "ko")
                        .queryParam("key", apiKey)
                        .build()
                }
                .retrieve()
                .body(JsonNode::class.java)

            extractLocation(response)
        } catch (e: RestClientException) {
            log.warn("[MARKER] reverse geocoding fallback reason={}", e.message)
            null
        }
    }

    private fun extractLocation(response: JsonNode?): ReverseGeocodingResult? {
        if (response == null || response.path("status").asText() != "OK") {
            return null
        }

        val firstResult = response.path("results").path(0)
        if (firstResult.isMissingNode) {
            return null
        }

        val addressComponents = firstResult.path("address_components")
        val country = normalizeCountryName(findLongNameByType(addressComponents, "country"))
        val city = extractCityName(addressComponents)
        val regionName = extractRegionName(addressComponents)

        return if (StringUtils.hasText(regionName)) {
            ReverseGeocodingResult(country, city, trimPlaceName(regionName))
        } else {
            ReverseGeocodingResult(
                country,
                city,
                trimPlaceName(firstResult.path("formatted_address").asText(null))
            )
        }
    }

    private fun normalizeCountryName(country: String?): String? {
        if (!StringUtils.hasText(country)) {
            return null
        }

        return when (country) {
            "대한민국", "Republic of Korea", "South Korea" -> "한국"
            else -> country
        }
    }

    private fun extractCityName(addressComponents: JsonNode): String? {
        val locality = findLongNameByType(addressComponents, "locality")
        return if (StringUtils.hasText(locality)) {
            locality
        } else {
            findLongNameByType(addressComponents, "administrative_area_level_1")
        }
    }

    private fun extractRegionName(addressComponents: JsonNode): String? {
        if (!addressComponents.isArray) {
            return null
        }

        val regionParts = mutableListOf<String>()
        for (regionType in REGION_TYPE_PRIORITY) {
            val regionPart = findLongNameByType(addressComponents, regionType)
            if (StringUtils.hasText(regionPart) && regionPart !in regionParts) {
                regionParts.add(regionPart!!)
            }
        }

        return regionParts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun findLongNameByType(addressComponents: JsonNode, targetType: String): String? {
        for (component in addressComponents) {
            if (hasType(component.path("types"), targetType)) {
                return component.path("long_name").asText(null)
            }
        }
        return null
    }

    private fun hasType(types: JsonNode, targetType: String): Boolean {
        if (!types.isArray) {
            return false
        }
        return types.any { type -> targetType == type.asText() }
    }

    private fun trimPlaceName(placeName: String?): String? {
        if (!StringUtils.hasText(placeName)) {
            return null
        }
        return placeName!!.take(MAX_PLACE_NAME_LENGTH)
    }

    private companion object {
        const val MAX_PLACE_NAME_LENGTH = 100
        val REGION_TYPE_PRIORITY = listOf(
            "administrative_area_level_1",
            "administrative_area_level_2",
            "locality",
            "sublocality_level_1",
            "sublocality_level_2",
            "sublocality_level_3"
        )
    }
}
