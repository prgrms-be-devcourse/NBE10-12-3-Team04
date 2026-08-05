package com.triptrace.domain.image.image.dto.response

import com.triptrace.domain.image.image.entity.UploadStatus

data class ImageUploadResponse(
    val fileName: String?, val id: Long?, val originalFileUrl: String?,
    val thumbnailUrl: String?, val mimeType: String?, val uploadStatus: UploadStatus?, val message: String?,
) {
    fun fileName() = fileName; fun id() = id; fun originalFileUrl() = originalFileUrl
    fun thumbnailUrl() = thumbnailUrl; fun mimeType() = mimeType; fun uploadStatus() = uploadStatus; fun message() = message
}
