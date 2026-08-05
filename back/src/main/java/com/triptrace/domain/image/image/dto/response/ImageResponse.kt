package com.triptrace.domain.image.image.dto.response

data class ImageResponse(
    val id: Long?,
    val ownerId: Long?,
    val tripId: Long?,
    val postId: Long?,
    val originalUrl: String?,
    val thumbnailUrl: String?,
)
