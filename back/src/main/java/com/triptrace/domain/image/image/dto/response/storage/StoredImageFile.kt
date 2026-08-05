package com.triptrace.domain.image.image.dto.response.storage

data class StoredImageFile(
    val imageFileUrl: String?,
    val thumbnailImageFileUrl: String?,
    val fileSize: Long?,
    val mimeType: String?,
)
