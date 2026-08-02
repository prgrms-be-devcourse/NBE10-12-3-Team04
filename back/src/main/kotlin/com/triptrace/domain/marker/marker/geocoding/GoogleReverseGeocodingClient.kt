package com.triptrace.domain.marker.marker.geocoding

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.JsonNode
import java.math.BigDecimal

@Component
class GoogleReverseGeocodingClient : ReverseGeocodingClient {
    private val restClient: RestClient
    private val apiKey: String

    @Autowired
    constructor(
        @Value("\${custom.google.maps.api-key:}") apiKey: String,
    ) : this(apiKey, RestClient.builder().baseUrl("https://maps.googleapis.com").build())
    constructor(apiKey: String, restClient: RestClient) {
        this.apiKey = apiKey
        this.restClient = restClient
    }

    override fun findPlaceName(
        latitude: BigDecimal,
        longitude: BigDecimal,
    ): String? = findLocation(latitude, longitude)?.placeName

    override fun findLocation(
        latitude: BigDecimal,
        longitude: BigDecimal,
    ): ReverseGeocodingResult? {
        if (!StringUtils.hasText(apiKey)) return null
        return try {
            val response =
                restClient
                    .get()
                    .uri {
                        it
                            .path(
                                "/maps/api/geocode/json",
                            ).queryParam("latlng", "$latitude,$longitude")
                            .queryParam("language", "ko")
                            .queryParam("key", apiKey)
                            .build()
                    }.retrieve()
                    .body(JsonNode::class.java)
            extractLocation(response)
        } catch (_: RestClientException) {
            null
        }
    }

    private fun extractLocation(response: JsonNode?): ReverseGeocodingResult? {
        if (response == null || response.path("status").asText() != "OK") return null
        val first = response.path("results").path(0)
        if (first.isMissingNode) return null
        val components = first.path("address_components")
        val country = normalizeCountryName(findLongNameByType(components, "country"))
        val city =
            findLongNameByType(components, "locality")?.takeIf(StringUtils::hasText)
                ?: findLongNameByType(components, "administrative_area_level_1")
        val placeName = extractRegionName(components) ?: first.path("formatted_address").asText(null)
        return ReverseGeocodingResult(country, city, trimPlaceName(placeName))
    }

    private fun normalizeCountryName(country: String?): String? =
        when (country?.takeIf(StringUtils::hasText)) {
            "대한민국", "Republic of Korea", "South Korea" -> "한국"
            else -> country
        }

    private fun extractRegionName(components: JsonNode): String? {
        if (!components.isArray) return null
        val parts = REGION_TYPES.mapNotNull { findLongNameByType(components, it)?.takeIf(StringUtils::hasText) }.distinct()
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun findLongNameByType(
        components: JsonNode,
        target: String,
    ): String? =
        components
            .firstOrNull { component ->
                component.path("types").let { types ->
                    types.isArray &&
                        types.any { it.asText() == target }
                }
            }?.path("long_name")
            ?.asText(null)

    private fun trimPlaceName(name: String?): String? = name?.takeIf(StringUtils::hasText)?.take(100)

    companion object {
        private val REGION_TYPES =
            listOf(
                "administrative_area_level_1",
                "administrative_area_level_2",
                "locality",
                "sublocality_level_1",
                "sublocality_level_2",
                "sublocality_level_3",
            )
    }
}
