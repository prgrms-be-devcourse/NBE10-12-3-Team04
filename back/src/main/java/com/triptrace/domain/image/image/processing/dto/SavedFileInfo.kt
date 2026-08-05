package com.triptrace.domain.image.image.processing.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SavedFileInfo(@field:NotNull val servingUrl: String, @field:NotNull val thumbnailUrl: String, val size: Long?, @field:NotBlank val mimeType: String) { fun servingUrl() = servingUrl; fun thumbnailUrl() = thumbnailUrl; fun size() = size; fun mimeType() = mimeType }
