package com.triptrace.domain.trip.trip.repository

import com.triptrace.domain.trip.trip.entity.Trip
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface TripRepository : JpaRepository<Trip, Long> {
    fun findByOwnerId(ownerId: Long): List<Trip>

    fun findByOwnerIdOrderByCreatedAtDescIdDesc(
        ownerId: Long,
        pageable: Pageable,
    ): Page<Trip>

    fun findByVisibilityTrue(): List<Trip>

    fun findByRepresentativeImageId(representativeImageId: Long): List<Trip>

    fun findByVisibilityTrueOrderByCreatedAtDescIdDesc(pageable: Pageable): Page<Trip>

    @Query(
        """SELECT tl.trip FROM TripLike tl WHERE tl.trip.visibility = true AND tl.createdAt >= :likedSince GROUP BY tl.trip ORDER BY COUNT(tl.id) DESC, MAX(tl.trip.createdAt) DESC, MAX(tl.trip.id) DESC""",
    )
    fun findTop10PublicTripsByRecentLikeCount(
        @Param("likedSince") likedSince: LocalDateTime,
    ): List<Trip>

    fun findByVisibilityTrueOrderByCreatedAtDesc(): List<Trip>

    @Query(
        """SELECT tl.trip, COUNT(tl.id) FROM TripLike tl WHERE tl.trip.visibility = true AND tl.createdAt >= :likedSince GROUP BY tl.trip ORDER BY COUNT(tl.id) DESC, MAX(tl.trip.createdAt) DESC, MAX(tl.trip.id) DESC""",
    )
    fun findWeeklyTrendingTrips(
        @Param("likedSince") likedSince: LocalDateTime,
        pageable: Pageable,
    ): List<Array<Any>>

    @Query(
        """SELECT t.country, t.city, COUNT(t.id), SUM(t.likeCount) FROM Trip t WHERE t.visibility = true AND t.country IS NOT NULL AND TRIM(t.country) <> '' AND t.city IS NOT NULL AND TRIM(t.city) <> '' GROUP BY t.country, t.city ORDER BY SUM(t.likeCount) DESC, COUNT(t.id) DESC, t.country ASC, t.city ASC""",
    )
    fun findPopularDestinations(pageable: Pageable): List<Array<Any>>

    fun findFirstByVisibilityTrueAndCountryAndCityOrderByLikeCountDescCreatedAtDescIdDesc(
        country: String,
        city: String,
    ): Optional<Trip>
}
