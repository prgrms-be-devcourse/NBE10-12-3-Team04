package com.triptrace.domain.trip.trip.repository;

import com.triptrace.domain.trip.trip.dto.TripSearchCondition;
import com.triptrace.domain.trip.trip.dto.TripSearchLocation;
import com.triptrace.domain.trip.trip.entity.Trip;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TripSearchRepository {

    Page<Trip> search(
        TripSearchCondition condition,
        Pageable pageable
    );

    List<TripSearchLocation> findPublicLocations();
}
