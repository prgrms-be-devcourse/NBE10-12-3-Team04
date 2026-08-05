package com.triptrace.domain.image.image.controller

import com.triptrace.domain.image.image.application.ImageDeleteUseCase
import com.triptrace.domain.image.image.application.ImageModifyUseCase
import com.triptrace.domain.image.image.application.ImageSearchUseCase
import com.triptrace.domain.image.image.application.ImageUploadUseCase
import com.triptrace.domain.image.image.dto.response.ImageResponse
import com.triptrace.domain.image.image.dto.response.ImageUploadResponse
import com.triptrace.global.app.Domain
import com.triptrace.global.rsData.RsData
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1")
class ApiV1ImageController(
    private val imageUploadUseCase: ImageUploadUseCase,
    private val imageDeleteUseCase: ImageDeleteUseCase,
    private val imageModifyUseCase: ImageModifyUseCase,
    private val imageSearchUseCase: ImageSearchUseCase,
) {
    @PostMapping("/trips/{tripId}/images")
    fun upload(
        @AuthenticationPrincipal ownerId: Long,
        @PathVariable tripId: Long,
        @RequestParam images: Array<MultipartFile>,
    ): RsData<List<ImageUploadResponse>> = RsData(
        successCode,
        "업로드 되었습니다.",
        imageUploadUseCase.uploadImages(ownerId, tripId, images),
    )

    @PostMapping("/trips/{tripId}/posts/{postId}/images")
    fun upload(
        @AuthenticationPrincipal ownerId: Long,
        @PathVariable tripId: Long,
        @PathVariable postId: Long,
        @RequestParam images: Array<MultipartFile>,
    ): RsData<List<ImageUploadResponse>> = RsData(
        successCode,
        "업로드 되었습니다.",
        imageUploadUseCase.uploadImages(ownerId, tripId, postId, images),
    )

    @DeleteMapping("/trips/{tripId}/posts/{postId}/images/{imageId}")
    fun delete(
        @AuthenticationPrincipal ownerId: Long,
        @PathVariable tripId: Long,
        @PathVariable postId: Long,
        @PathVariable imageId: Long,
    ): RsData<Nothing?> {
        imageDeleteUseCase.deleteById(ownerId, tripId, postId, imageId)
        return RsData(successCode, "삭제 되었습니다.", null)
    }

    @DeleteMapping("/trips/{tripId}/images/{imageId}")
    fun delete(
        @AuthenticationPrincipal ownerId: Long,
        @PathVariable tripId: Long,
        @PathVariable imageId: Long,
    ): RsData<Nothing?> {
        imageDeleteUseCase.deleteById(ownerId, tripId, imageId)
        return RsData(successCode, "삭제 되었습니다.", null)
    }

    @DeleteMapping("/trips/{tripId}/posts/{postId}/images")
    fun delete(
        @AuthenticationPrincipal ownerId: Long,
        @PathVariable tripId: Long,
        @PathVariable postId: Long,
        @RequestParam imageUrl: String,
    ): RsData<Nothing?> {
        imageDeleteUseCase.deleteByUrl(ownerId, tripId, postId, imageUrl)
        return RsData(successCode, "삭제 되었습니다.", null)
    }

    @PatchMapping("/trips/{tripId}/images")
    fun modify(
        @AuthenticationPrincipal ownerId: Long,
        @PathVariable tripId: Long,
        @RequestParam postId: Long,
        @RequestParam imageId: Long,
    ): RsData<*> = RsData(
        successCode,
        "수정 되었습니다.",
        imageModifyUseCase.modifyById(ownerId, tripId, postId, imageId),
    )

    @GetMapping("/images")
    fun list(@AuthenticationPrincipal ownerId: Long): RsData<List<ImageResponse>> = RsData(
        successCode,
        "SUCCESS",
        imageSearchUseCase.getImages(ownerId),
    )

    @PatchMapping("/trips/{tripId}/images/{imageId}/unassign")
    fun unassign(
        @AuthenticationPrincipal ownerId: Long,
        @PathVariable tripId: Long,
        @PathVariable imageId: Long,
    ): RsData<*> = RsData(
        successCode,
        "수정 되었습니다.",
        imageModifyUseCase.unassign(ownerId, tripId, imageId),
    )

    private companion object {
        private val successCode = "200-${Domain.IMAGE.code}"
    }
}
