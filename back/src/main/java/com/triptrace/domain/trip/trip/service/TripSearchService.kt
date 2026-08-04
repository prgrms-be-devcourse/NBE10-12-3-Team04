package com.triptrace.domain.trip.trip.service

import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.post.post.repository.PostRepository
import com.triptrace.domain.trip.trip.dto.TripSearchCondition
import com.triptrace.domain.trip.trip.dto.TripSearchLocationResponse
import com.triptrace.domain.trip.trip.dto.TripSearchResponse
import com.triptrace.domain.trip.trip.dto.TripSearchScope
import com.triptrace.domain.trip.trip.dto.TripSearchSort
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.domain.trip.trip.error.TripErrorCode
import com.triptrace.domain.trip.trip.repository.TripSearchRepository
import com.triptrace.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.util.Locale
import java.util.TreeMap
import java.util.TreeSet

@Service
class TripSearchService(
    private val tripSearchRepository: TripSearchRepository,
    private val postRepository: PostRepository
) {
    @Transactional(readOnly = true)
    fun search(
        keyword: String?,
        scope: TripSearchScope?,
        country: String?,
        city: String?,
        sort: TripSearchSort?,
        pageable: Pageable
    ): Page<TripSearchResponse> {
        val normalizedCountry = normalizeFilter(country)
        val normalizedCity = normalizeFilter(city)
        if (normalizedCity != null && normalizedCountry == null) {
            throw ServiceException(TripErrorCode.CITY_REQUIRES_COUNTRY)
        }

        val condition = TripSearchCondition(
            tokenize(keyword),
            scope ?: TripSearchScope.ALL,
            normalizedCountry,
            normalizedCity,
            sort ?: TripSearchSort.LATEST
        )
        val trips = tripSearchRepository.search(condition, pageable)
        val postsByTripId = findPostsByTripId(trips.content)

        return trips.map { trip ->
            toResponse(trip, postsByTripId.getOrDefault(trip.getId(), emptyList()))
        }
    }

    @Transactional(readOnly = true)
    fun findLocations(): List<TripSearchLocationResponse> {
        val citiesByCountry = TreeMap<String, TreeSet<String>>()
        tripSearchRepository.findPublicLocations().forEach { location ->
            val country = normalizeFilter(location.country)
            val city = normalizeFilter(location.city)
            if (country != null && city != null) {
                citiesByCountry.computeIfAbsent(country) { TreeSet() }.add(city)
            }
        }

        return citiesByCountry.map { (country, cities) ->
            TripSearchLocationResponse(country, cities.toList())
        }
    }

    private fun tokenize(keyword: String?): List<String> {
        if (!StringUtils.hasText(keyword)) {
            return emptyList()
        }

        val normalized = keyword!!
            .replace(Regex("[%_\\\\]"), " ")
            .trim()
            .lowercase(Locale.ROOT)
        if (normalized.isEmpty()) {
            return emptyList()
        }

        return normalized.split(Regex("\\s+"))
            .toCollection(LinkedHashSet())
            .toList()
    }

    private fun normalizeFilter(value: String?): String? =
        if (StringUtils.hasText(value)) value!!.trim() else null

    private fun findPostsByTripId(trips: List<Trip>): Map<Long, List<Post>> {
        if (trips.isEmpty()) {
            return emptyMap()
        }

        val tripIds = trips.map { it.getId() }
        return postRepository.findByTripIdInOrderByDateAscIdAsc(tripIds)
            .groupBy { it.getTrip().getId() }
    }

    private fun toResponse(trip: Trip, posts: List<Post>): TripSearchResponse {
        val thumbnailUrl = trip.representativeImage?.getThumbnailUrl()
        val previewText = if (posts.isEmpty()) null else createPreview(posts.first().getMemo())

        return TripSearchResponse(
            trip.getId(),
            trip.title,
            thumbnailUrl,
            trip.startDate,
            trip.endDate,
            trip.country,
            trip.city,
            previewText
        )
    }

    private fun createPreview(memo: String?): String? {
        if (!StringUtils.hasText(memo)) {
            return null
        }

        val normalized = memo!!.replace(Regex("\\s+"), " ").trim()
        if (normalized.length <= PREVIEW_MAX_LENGTH) {
            return normalized
        }
        return normalized.substring(0, PREVIEW_MAX_LENGTH - 3) + "..."
    }

    companion object {
        private const val PREVIEW_MAX_LENGTH = 100
    }
}
