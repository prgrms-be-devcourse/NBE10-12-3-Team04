package com.triptrace.domain.image.image.processing.dto

import java.time.LocalDateTime

data class ImageDateTime(val dateTime: LocalDateTime?, val timeZone: String?) { fun dateTime() = dateTime; fun timeZone() = timeZone }
