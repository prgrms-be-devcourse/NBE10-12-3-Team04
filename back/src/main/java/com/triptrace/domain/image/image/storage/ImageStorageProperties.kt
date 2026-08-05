package com.triptrace.domain.image.image.storage

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "custom")
@Validated
data class ImageStorageProperties(
    @field:NotNull @field:Valid val upload: Upload,
    @field:NotNull @field:Valid val thumbnail: Thumbnail,
    @field:NotNull @field:Valid val ext: Ext,
) {
    fun upload() = upload; fun thumbnail() = thumbnail; fun ext() = ext
    data class Upload(@field:NotBlank val path: String, @field:NotBlank val servingPath: String, @field:NotBlank val thumbnailPath: String, @field:NotBlank val profilePath: String, @field:NotBlank val publicPrefix: String) { fun path() = path; fun servingPath() = servingPath; fun thumbnailPath() = thumbnailPath; fun profilePath() = profilePath; fun publicPrefix() = publicPrefix }
    data class Thumbnail(@field:Min(1) val width: Int, @field:Min(1) val height: Int) { fun width() = width; fun height() = height }
    data class Ext(@field:NotBlank val jpg: String) { fun jpg() = jpg }
}
