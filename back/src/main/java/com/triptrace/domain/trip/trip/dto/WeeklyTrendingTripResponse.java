package com.triptrace.domain.trip.trip.dto;

public record WeeklyTrendingTripResponse(
    TripResponse trip,
    long weeklyLikeCount
) {
}
