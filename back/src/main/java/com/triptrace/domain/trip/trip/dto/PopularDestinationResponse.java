package com.triptrace.domain.trip.trip.dto;

public record PopularDestinationResponse(
    String country,
    String city,
    long tripCount,
    String thumbnailUrl,
    Long representativeTripId
) {
}
