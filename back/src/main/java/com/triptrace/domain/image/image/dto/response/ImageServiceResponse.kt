package com.triptrace.domain.image.image.dto.response

import com.triptrace.domain.image.image.entity.UploadStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class ImageServiceResponse(
    val id: Long?,
    val ownerId: Long?,
    val tripId: Long?,
    val postId: Long?,
    val originalFileUrl: String?,
    val thumbnailUrl: String?,
    val mimeType: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val capturedAt: LocalDateTime?,
    val deviceInfo: String?,
    val uploadStatus: UploadStatus?,
)
