package com.triptrace.domain.image.image.processing.dto

import com.triptrace.domain.image.image.processing.ExifOrientation

data class ImageExifIF(
    val orientation: ExifOrientation?,
    val device: String?,
    val maker: String?,
)
