package com.triptrace.domain.image.image.processing.dto

data class ImageWidthHeight(
    val width: Int?,
    val height: Int?,
) {
    fun width() = width
    fun height() = height
}
