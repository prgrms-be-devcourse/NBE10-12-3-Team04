package com.triptrace.domain.trip.tripAuto.dto

import java.math.BigDecimal
import java.time.LocalDate

// AutoRecord API가 생성한 Post/Marker와 사용/제외된 이미지 수를 클라이언트에 알려주는 응답이다.
@JvmRecord
data class TripAutoRecordResponse(
    val tripId: Long?,
    val generatedPostCount: Int,
    val generatedMarkerCount: Int,
    val usedImageCount: Int,
    val skippedImageCount: Int,
    val records: List<GeneratedRecord>
) {
    // 클러스터 하나가 Post 하나와 Marker 하나로 변환된 결과를 표현
    @JvmRecord
    data class GeneratedRecord(
        val postId: Long?,
        val markerId: Long?,
        val representativeImageId: Long?,
        val representativeThumbnailUrl: String?,
        val title: String?,
        // 역지오코딩에 실패하면 장소명을 못 채우므로 null이 될 수 있다.
        val location: String?,
        val date: LocalDate?,
        val centerLat: BigDecimal?,
        val centerLng: BigDecimal?,
        val imageIds: List<Long>
    )
}
