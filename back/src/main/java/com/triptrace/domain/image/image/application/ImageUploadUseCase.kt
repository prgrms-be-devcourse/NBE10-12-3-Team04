package com.triptrace.domain.image.image.application

import com.triptrace.domain.image.image.dto.response.ImageUploadResponse
import com.triptrace.domain.image.image.dto.response.storage.StoredImageFile
import com.triptrace.domain.image.image.error.ImageErrorCode
import com.triptrace.domain.image.image.exception.ImageProcessException
import com.triptrace.domain.image.image.mapper.ImageMapper
import com.triptrace.domain.image.image.processing.ImageInfo
import com.triptrace.domain.image.image.processing.ImageMetadataExtractor
import com.triptrace.domain.image.image.processing.dto.SavedFileInfo
import com.triptrace.domain.image.image.service.ImageService
import com.triptrace.domain.image.image.storage.ImageFileStorage
import com.triptrace.domain.member.member.entity.Member
import com.triptrace.domain.member.member.service.MemberService
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.post.post.service.PostService
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.domain.trip.trip.service.TripService
import com.triptrace.global.app.Domain
import com.triptrace.global.exception.ServiceException
import jakarta.validation.constraints.NotEmpty
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@Service
class ImageUploadUseCase(
    private val imageService: ImageService,
    private val imageMetadataExtractor: ImageMetadataExtractor,
    private val imageFileStorage: ImageFileStorage,
    private val tripService: TripService,
    private val memberService: MemberService,
    private val postService: PostService,
) {
    private fun extract(imageFile: MultipartFile): ImageInfo = try {
        imageMetadataExtractor.extract(imageFile.bytes)
    } catch (e: IOException) {
        log.warn("[{}] image processor fallback reason: {}", Domain.IMAGE.name, e.message)
        ImageInfo()
    } catch (e: ImageProcessException) {
        log.warn("[{}] image processor fallback reason: {}", Domain.IMAGE.name, e.message)
        ImageInfo()
    }

    private fun upload(owner: Member, trip: Trip, imageFile: MultipartFile): ImageUploadResponse =
        upload(owner, trip, null, imageFile)

    private fun upload(
        owner: Member,
        trip: Trip,
        post: Post?,
        imageFile: MultipartFile,
    ): ImageUploadResponse {
        if (imageFile.isEmpty) return ImageMapper.toUploadResponse(null, null, "EMPTY_FILE")

        log.info("[{}] upload start owner: {}, trip: {}", Domain.IMAGE.name, owner.id, trip.id)
        val fileName = imageFile.originalFilename
        val imageInfo = extract(imageFile)
        val savedFileInfo: SavedFileInfo
        val storedImageFile: StoredImageFile

        try {
            savedFileInfo = imageFileStorage.saveImageWithThumbnail(
                imageFile.bytes,
                imageInfo.orientation,
            )
            storedImageFile = ImageMapper.toStoredImageFile(savedFileInfo)
        } catch (e: IOException) {
            log.warn("[{}] image upload use case fallback reason: {}", Domain.IMAGE.name, e.message)
            return ImageMapper.toUploadResponse(fileName, null, "FILE SAVE FAILED")
        } catch (e: ImageProcessException) {
            log.warn("[{}] image upload use case fallback reason: {}", Domain.IMAGE.name, e.message)
            return ImageMapper.toUploadResponse(fileName, null, "FILE SAVE FAILED")
        }

        val image = ImageMapper.toEntity(owner, trip, post, imageInfo, storedImageFile)
        val imageServiceResponse = try {
            imageService.create(image)
        } catch (e: IllegalArgumentException) {
            imageFileStorage.cleanUp(savedFileInfo)
            log.warn("[{}] image upload use case fallback reason: {}", Domain.IMAGE.name, e.message)
            return ImageMapper.toUploadResponse(fileName, null, "SERVER SAVE FAILED")
        } catch (e: OptimisticLockingFailureException) {
            imageFileStorage.cleanUp(savedFileInfo)
            log.warn("[{}] image upload use case fallback reason: {}", Domain.IMAGE.name, e.message)
            return ImageMapper.toUploadResponse(fileName, null, "SERVER SAVE FAILED")
        }

        return ImageMapper.toUploadResponse(fileName, imageServiceResponse)
    }

    fun uploadImages(
        ownerId: Long,
        tripId: Long,
        @NotEmpty images: Array<MultipartFile>,
    ): List<ImageUploadResponse> {
        validateImagesRequest(images)
        val owner = memberService.findById(ownerId)
        val trip = tripService.findOwnedTrip(tripId, owner.id)
        return images.map { upload(owner, trip, it) }
    }

    fun uploadImages(
        ownerId: Long,
        tripId: Long,
        postId: Long,
        @NotEmpty images: Array<MultipartFile>,
    ): List<ImageUploadResponse> {
        validateImagesRequest(images)
        val owner = memberService.findById(ownerId)
        val trip = tripService.findOwnedTrip(tripId, owner.id)
        val post = postService.getPost(trip, postId)
        return images.map { upload(owner, trip, post, it) }
    }

    private fun validateImagesRequest(images: Array<MultipartFile>?) {
        if (images.isNullOrEmpty()) throw ServiceException(ImageErrorCode.NO_IMAGE)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(ImageUploadUseCase::class.java)
    }
}
