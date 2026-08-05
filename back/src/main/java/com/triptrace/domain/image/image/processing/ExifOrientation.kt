package com.triptrace.domain.image.image.processing

enum class ExifOrientation(val exifValue: Int, val rotationDegrees: Int) {
    NORMAL(1, 0), ROTATE_180(3, 180), ROTATE_90_CW(6, 90), ROTATE_270_CW(8, 270);

    companion object {
        @JvmStatic fun fromExifValue(exifValue: Int) = entries.firstOrNull { it.exifValue == exifValue } ?: NORMAL
    }
}
