package com.triptrace.domain.image.image.processing

import java.time.LocalDateTime

class ImageInfo(
    val width: Int? = 0,
    val height: Int? = 0,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val capturedAt: LocalDateTime? = null,
    val timeZone: String? = null,
    val model: String? = null,
    val maker: String? = null,
    val orientation: ExifOrientation = ExifOrientation.NORMAL,
    val fileSize: Long? = 0L,
)
