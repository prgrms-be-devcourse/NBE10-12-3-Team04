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
    private val postRepository: PostRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        keyword: String?,
        scope: TripSearchScope?,
        country: String?,
        city: String?,
        sort: TripSearchSort?,
        pageable: Pageable,
    ): Page<TripSearchResponse> {
        val normalizedCountry = normalizeFilter(country)
        val normalizedCity = normalizeFilter(city)
        if (normalizedCity != null && normalizedCountry == null) throw ServiceException(TripErrorCode.CITY_REQUIRES_COUNTRY)
        val condition =
            TripSearchCondition(
                tokenize(keyword),
                scope ?: TripSearchScope.ALL,
                normalizedCountry,
                normalizedCity,
                sort ?: TripSearchSort.LATEST,
            )
        val trips = tripSearchRepository.search(condition, pageable)
        val postsByTripId = findPostsByTripId(trips.content)
        return trips.map { toResponse(it, postsByTripId[it.id].orEmpty()) }
    }

    @Transactional(readOnly = true)
    fun findLocations(): List<TripSearchLocationResponse> {
        val citiesByCountry = TreeMap<String, TreeSet<String>>()
        tripSearchRepository.findPublicLocations().forEach {
            val country = normalizeFilter(it.country)
            val city = normalizeFilter(it.city)
            if (country != null && city != null) citiesByCountry.computeIfAbsent(country) { TreeSet() }.add(city)
        }
        return citiesByCountry.map { TripSearchLocationResponse(it.key, it.value.toList()) }
    }

    private fun tokenize(keyword: String?): List<String> {
        if (!StringUtils.hasText(keyword)) return emptyList()
        val normalized = keyword!!.replace(Regex("[%_\\\\]"), " ").trim().lowercase(Locale.ROOT)
        return if (normalized.isEmpty()) emptyList() else normalized.split(Regex("\\s+")).toCollection(LinkedHashSet()).toList()
    }

    private fun normalizeFilter(value: String?): String? = value?.trim()?.takeIf(String::isNotEmpty)

    private fun findPostsByTripId(trips: List<Trip>): Map<Long?, List<Post>> =
        if (trips.isEmpty()) {
            emptyMap()
        } else {
            postRepository
                .findByTripIdInOrderByDateAscIdAsc(
                    trips.mapNotNull {
                        it.id
                    },
                ).groupBy { it.trip.id }
        }

    private fun toResponse(
        trip: Trip,
        posts: List<Post>,
    ) = TripSearchResponse(
        trip.id,
        trip.title,
        trip.representativeImage?.thumbnailUrl,
        trip.startDate,
        trip.endDate,
        trip.country,
        trip.city,
        posts.firstOrNull()?.memo?.let(::createPreview),
    )

    private fun createPreview(memo: String): String? {
        if (!StringUtils.hasText(memo)) return null
        val normalized = memo.replace(Regex("\\s+"), " ").trim()
        return if (normalized.length <= 100) normalized else normalized.substring(0, 97) + "..."
    }
}
