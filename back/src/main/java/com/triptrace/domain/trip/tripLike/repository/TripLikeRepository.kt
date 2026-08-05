package com.triptrace.domain.trip.tripLike.repository

import com.triptrace.domain.trip.tripLike.entity.TripLike
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TripLikeRepository : JpaRepository<TripLike, Long> {

    fun existsByMemberIdAndTripId(memberId: Long, tripId: Long): Boolean

    fun findByMemberIdAndTripId(memberId: Long, tripId: Long): Optional<TripLike>

    fun countByTripId(tripId: Long): Long

    // 여행기가 지워질 때 딸린 좋아요를 함께 정리한다.
    fun deleteByTripId(tripId: Long)
}
