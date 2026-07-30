package com.triptrace.domain.image.image.application;

import java.util.List;
import org.springframework.stereotype.Component;
import com.triptrace.domain.image.image.dto.response.ImageResponse;
import com.triptrace.domain.image.image.mapper.ImageMapper;
import com.triptrace.domain.image.image.service.ImageService;

@Component
public class ImageSearchUseCase {
    private final ImageService imageService;

    public List<ImageResponse> getImages(Long ownerId) {
        return imageService.findWithOwner(ownerId).stream().map(ImageMapper::toImageResponse).toList();
    }

    @java.lang.SuppressWarnings("all")
    public ImageSearchUseCase(final ImageService imageService) {
        this.imageService = imageService;
    }
}
