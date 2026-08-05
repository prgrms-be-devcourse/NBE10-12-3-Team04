package com.triptrace.domain.image.image.processing

import java.time.LocalDateTime

class ImageInfo {
    var width: Int? = 0; private set
    var height: Int? = 0; private set
    var longitude: Double? = null; private set
    var latitude: Double? = null; private set
    var capturedAt: LocalDateTime? = null; private set
    var timeZone: String? = null; private set
    var model: String? = null; private set
    var maker: String? = null; private set
    var orientation: ExifOrientation? = ExifOrientation.NORMAL; private set
    var fileSize: Long? = 0L; private set
    fun setWidth(value: Int?) { width = value }; fun setHeight(value: Int?) { height = value }
    fun setLongitude(value: Double?) { longitude = value }; fun setLatitude(value: Double?) { latitude = value }
    fun setCapturedAt(value: LocalDateTime?) { capturedAt = value }; fun setTimeZone(value: String?) { timeZone = value }
    fun setModel(value: String?) { model = value }; fun setMaker(value: String?) { maker = value }
    fun setOrientation(value: ExifOrientation?) { orientation = value }; fun setFileSize(value: Long?) { fileSize = value }
}
