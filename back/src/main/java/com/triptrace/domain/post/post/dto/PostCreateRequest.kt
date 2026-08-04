package com.triptrace.domain.post.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

@JvmRecord
data class PostCreateRequest( //tripId는 URL에서 받음
    @field:NotNull val date: LocalDate?,
    val time: LocalTime?,
    @field:NotBlank @field:Size(max = 100) val title: String?,
    val memo: String?
)
