package com.triptrace.domain.marker.marker.place

import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.JsonNode
import java.math.BigDecimal

@Component
class GooglePlacesClient {
    private val restClient: RestClient
    private val apiKey: String

    @Autowired
    constructor(
        @Value("\${custom.google.maps.api-key:}") apiKey: String,
    ) : this(apiKey, RestClient.builder().baseUrl("https://places.googleapis.com").build())
    constructor(apiKey: String, restClient: RestClient) {
        this.apiKey = apiKey
        this.restClient = restClient
    }

    fun findNearbyPlaces(
        latitude: BigDecimal?,
        longitude: BigDecimal?,
    ): List<PlaceCandidateResponse> {
        if (!StringUtils.hasText(apiKey) || latitude == null || longitude == null) return emptyList()
        return request(
            "/v1/places:searchNearby",
            mapOf(
                "maxResultCount" to 5,
                "locationRestriction" to
                    mapOf("circle" to mapOf("center" to mapOf("latitude" to latitude, "longitude" to longitude), "radius" to 100.0)),
            ),
        )
    }

    fun searchPlaces(keyword: String?): List<PlaceCandidateResponse> {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(keyword)) return emptyList()
        return request("/v1/places:searchText", mapOf("textQuery" to keyword!!, "languageCode" to "ko", "maxResultCount" to 5))
    }

    private fun request(
        uri: String,
        body: Map<String, Any>,
    ): List<PlaceCandidateResponse> =
        try {
            extractPlaceCandidates(
                restClient
                    .post()
                    .uri(
                        uri,
                    ).contentType(
                        MediaType.APPLICATION_JSON,
                    ).header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(body)
                    .retrieve()
                    .body(JsonNode::class.java),
            )
        } catch (_: RestClientException) {
            emptyList()
        }

    private fun extractPlaceCandidates(response: JsonNode?): List<PlaceCandidateResponse> {
        val places = response?.path("places") ?: return emptyList()
        if (!places.isArray) return emptyList()
        return places.mapNotNull { place ->
            val name =
                place
                    .path("displayName")
                    .path("text")
                    .asText(null)
                    .takeIf(StringUtils::hasText) ?: return@mapNotNull null
            PlaceCandidateResponse(
                place
                    .path(
                        "id",
                    ).asText(
                        null,
                    ),
                name,
                place
                    .path(
                        "formattedAddress",
                    ).asText(
                        null,
                    ),
                toBigDecimal(
                    place.path("location").path("latitude"),
                ),
                toBigDecimal(place.path("location").path("longitude")),
                place
                    .path("types")
                    .takeIf {
                        it.isArray
                    }?.mapNotNull { it.asText(null).takeIf(StringUtils::hasText) }
                    .orEmpty(),
            )
        }
    }

    private fun toBigDecimal(node: JsonNode): BigDecimal? = node.asText(null)?.takeIf(StringUtils::hasText)?.let(::BigDecimal)

    companion object {
        private const val FIELD_MASK = "places.displayName,places.formattedAddress,places.location,places.id,places.types"
    }
}
