package com.triptrace.domain.post.post.dto

import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

@JvmRecord
data class PostModifyRequest(
    val date: LocalDate?,
    val time: LocalTime?,
    @field:Size(max = 100) val title: String?,
    val memo: String?,
)
