package com.triptrace.domain.trip.trip.repository

import com.triptrace.domain.trip.trip.dto.TripSearchCondition
import com.triptrace.domain.trip.trip.dto.TripSearchLocation
import com.triptrace.domain.trip.trip.entity.Trip
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TripSearchRepository {
    fun search(
        condition: TripSearchCondition,
        pageable: Pageable,
    ): Page<Trip>

    fun findPublicLocations(): List<TripSearchLocation>
}
