package com.triptrace.domain.image.image.dto.response.storage;


public record StoredImageFile(
    String imageFileUrl,
    String thumbnailImageFileUrl,
    Long fileSize,
    String mimeType
) {
}
