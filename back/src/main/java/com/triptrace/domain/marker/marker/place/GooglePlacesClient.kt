package com.triptrace.domain.marker.marker.place

import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse
import java.math.BigDecimal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.JsonNode

@Component
class GooglePlacesClient(
    private val apiKey: String,
    private val restClient: RestClient
) {

    @Autowired
    constructor(
        @Value("\${custom.google.maps.api-key:}") apiKey: String
    ) : this(
        apiKey,
        RestClient.builder()
            .baseUrl("https://places.googleapis.com")
            .build()
    )

    fun findNearbyPlaces(latitude: BigDecimal?, longitude: BigDecimal?): List<PlaceCandidateResponse> {
        if (!StringUtils.hasText(apiKey) || latitude == null || longitude == null) {
            return emptyList()
        }

        return try {
            val response = restClient.post()
                .uri("/v1/places:searchNearby")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(createRequestBody(latitude, longitude))
                .retrieve()
                .body(JsonNode::class.java)

            extractPlaceCandidates(response)
        } catch (_: RestClientException) {
            emptyList()
        }
    }

    fun searchPlaces(keyword: String?): List<PlaceCandidateResponse> {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(keyword)) {
            return emptyList()
        }

        return try {
            val response = restClient.post()
                .uri("/v1/places:searchText")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(createTextSearchRequestBody(keyword!!))
                .retrieve()
                .body(JsonNode::class.java)

            extractPlaceCandidates(response)
        } catch (_: RestClientException) {
            emptyList()
        }
    }

    private fun createRequestBody(latitude: BigDecimal, longitude: BigDecimal): Map<String, Any> =
        mapOf(
            "maxResultCount" to MAX_RESULT_COUNT,
            "locationRestriction" to mapOf(
                "circle" to mapOf(
                    "center" to mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude
                    ),
                    "radius" to SEARCH_RADIUS_METERS
                )
            )
        )

    private fun createTextSearchRequestBody(keyword: String): Map<String, Any> =
        mapOf(
            "textQuery" to keyword,
            "languageCode" to "ko",
            "maxResultCount" to MAX_RESULT_COUNT
        )

    private fun extractPlaceCandidates(response: JsonNode?): List<PlaceCandidateResponse> {
        val places = response?.path("places")
        if (places == null || !places.isArray) {
            return emptyList()
        }

        return buildList {
            for (place in places) {
                val name = place.path("displayName").path("text").asText(null)
                if (!StringUtils.hasText(name)) {
                    continue
                }

                add(
                    PlaceCandidateResponse(
                        place.path("id").asText(null),
                        name,
                        place.path("formattedAddress").asText(null),
                        toBigDecimal(place.path("location").path("latitude")),
                        toBigDecimal(place.path("location").path("longitude")),
                        extractTypes(place.path("types")).toMutableList()
                    )
                )
            }
        }
    }

    private fun extractTypes(types: JsonNode): List<String> {
        if (!types.isArray) {
            return emptyList()
        }

        return buildList {
            for (type in types) {
                val value = type.asText(null)
                if (StringUtils.hasText(value)) {
                    add(value!!)
                }
            }
        }
    }

    private fun toBigDecimal(node: JsonNode): BigDecimal? {
        val value = node.asText(null)
        return if (StringUtils.hasText(value)) BigDecimal(value) else null
    }

    private companion object {
        const val MAX_RESULT_COUNT = 5
        const val SEARCH_RADIUS_METERS = 100.0
        val FIELD_MASK = listOf(
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.id",
            "places.types"
        ).joinToString(",")
    }
}
