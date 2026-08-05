package com.triptrace.global.web;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.triptrace.domain.image.image.storage.ImageStorageProperties;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private final ImageStorageProperties.Upload upload;
    private final String servingImagesPath;
    private final String thumbnailImagesPath;
    private final String profileImagesPath;

    public StaticResourceConfig(
        ImageStorageProperties imageStorageProperties
    ) {
        this.upload = imageStorageProperties.getUpload();
        this.servingImagesPath = toResourceLocation(resolveUploadPath(upload.getServingPath()));
        this.thumbnailImagesPath = toResourceLocation(resolveUploadPath(upload.getThumbnailPath()));
        this.profileImagesPath = toResourceLocation(resolveUploadPath(upload.getProfilePath()));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(upload.getServingPath() + "/**")
            .addResourceLocations(servingImagesPath);
        registry.addResourceHandler(upload.getThumbnailPath() + "/**")
            .addResourceLocations(thumbnailImagesPath);
        registry.addResourceHandler(upload.getProfilePath() + "/**")
            .addResourceLocations(profileImagesPath);
    }

    private String toResourceLocation(String path) {
        return Path.of(path).toAbsolutePath().normalize().toUri().toString();
    }

    private String resolveUploadPath(String path) {
        return Path.of(upload.getPath(), path).toString();
    }
}
