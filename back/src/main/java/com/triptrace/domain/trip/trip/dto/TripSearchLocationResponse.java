package com.triptrace.domain.trip.trip.dto;

import java.util.List;

public record TripSearchLocationResponse(
    String country,
    List<String> cities
) {
}
