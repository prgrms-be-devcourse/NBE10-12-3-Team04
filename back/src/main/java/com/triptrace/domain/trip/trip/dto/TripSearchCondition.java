package com.triptrace.domain.trip.trip.dto;

import java.util.List;

// 검색 조건 생성
public record TripSearchCondition (
    List<String> tokens,
    TripSearchScope scope,
    String country,
    String city,
    TripSearchSort sort
    ){
    public  boolean hasKeyword(){
        return tokens != null && !tokens.isEmpty();
    }
}
