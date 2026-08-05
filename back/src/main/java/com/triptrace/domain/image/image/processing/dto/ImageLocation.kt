package com.triptrace.domain.image.image.processing.dto

data class ImageLocation(
    val latitude: Double?,
    val longitude: Double?,
) {
    fun latitude() = latitude
    fun longitude() = longitude
}
