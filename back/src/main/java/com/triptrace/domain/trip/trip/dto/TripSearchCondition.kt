package com.triptrace.domain.trip.trip.dto

// 검색 조건 생성
@JvmRecord
data class TripSearchCondition(
    val tokens: List<String>,
    val scope: TripSearchScope,
    val country: String?,
    val city: String?,
    val sort: TripSearchSort
) {
    fun hasKeyword(): Boolean = tokens.isNotEmpty()
}
