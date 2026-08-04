package com.triptrace.domain.trip.trip.repository;

import com.triptrace.domain.trip.trip.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByOwnerId(Long ownerId);

    Page<Trip> findByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId, Pageable pageable);

    List<Trip> findByVisibilityTrue();

    List<Trip> findByRepresentativeImageId(Long representativeImageId);

    Page<Trip> findByVisibilityTrueOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
        SELECT tl.trip
        FROM TripLike tl
        WHERE tl.trip.visibility = true
            AND tl.createdAt >= :likedSince
        GROUP BY tl.trip
        ORDER BY COUNT(tl.id) DESC, MAX(tl.trip.createdAt) DESC, MAX(tl.trip.id) DESC
        """)
    List<Trip> findTop10PublicTripsByRecentLikeCount(@Param("likedSince") LocalDateTime likedSince);

    // 공개여행기 중 createdAt 기준 내림차순 조회 퀴리 추가
    List<Trip> findByVisibilityTrueOrderByCreatedAtDesc();

    @Query("""
        SELECT tl.trip, COUNT(tl.id)
        FROM TripLike tl
        WHERE tl.trip.visibility = true
            AND tl.createdAt >= :likedSince
        GROUP BY tl.trip
        ORDER BY COUNT(tl.id) DESC, MAX(tl.trip.createdAt) DESC, MAX(tl.trip.id) DESC
        """)
    List<Object[]> findWeeklyTrendingTrips(
        @Param("likedSince") LocalDateTime likedSince,
        Pageable pageable
    );

    @Query("""
        SELECT t.country, t.city, COUNT(t.id), SUM(t.likeCount)
        FROM Trip t
        WHERE t.visibility = true
            AND t.country IS NOT NULL
            AND TRIM(t.country) <> ''
            AND t.city IS NOT NULL
            AND TRIM(t.city) <> ''
        GROUP BY t.country, t.city
        ORDER BY SUM(t.likeCount) DESC, COUNT(t.id) DESC, t.country ASC, t.city ASC
        """)
    List<Object[]> findPopularDestinations(Pageable pageable);

    Optional<Trip> findFirstByVisibilityTrueAndCountryAndCityOrderByLikeCountDescCreatedAtDescIdDesc(
        String country,
        String city
    );
}
