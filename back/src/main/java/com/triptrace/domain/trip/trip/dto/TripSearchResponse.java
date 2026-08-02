package com.triptrace.domain.trip.trip.dto;

import java.time.LocalDateTime;

// 검색 결과 DTO
public record TripSearchResponse (
    Long tripId,
    String title,
    String thumbnailUrl,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String country,
    String city,
    String previewText
){
}
