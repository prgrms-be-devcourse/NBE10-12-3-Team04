package com.triptrace.domain.image.image.application

import com.triptrace.domain.image.image.dto.response.ImageResponse
import com.triptrace.domain.image.image.mapper.ImageMapper
import com.triptrace.domain.image.image.service.ImageService
import org.springframework.stereotype.Component

@Component
class ImageSearchUseCase(private val imageService: ImageService) {
    fun getImages(ownerId: Long): List<ImageResponse> =
        imageService.findWithOwner(ownerId).map(ImageMapper::toImageResponse)
}
