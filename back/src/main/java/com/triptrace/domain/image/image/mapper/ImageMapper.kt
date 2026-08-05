package com.triptrace.domain.image.image.mapper

import com.triptrace.domain.image.image.dto.response.ImageResponse
import com.triptrace.domain.image.image.dto.response.ImageServiceResponse
import com.triptrace.domain.image.image.dto.response.ImageUploadResponse
import com.triptrace.domain.image.image.dto.response.storage.StoredImageFile
import com.triptrace.domain.image.image.entity.Image
import com.triptrace.domain.image.image.entity.UploadStatus
import com.triptrace.domain.image.image.processing.ImageInfo
import com.triptrace.domain.image.image.processing.dto.SavedFileInfo
import com.triptrace.domain.member.member.entity.Member
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.trip.trip.entity.Trip
import java.math.BigDecimal
import java.math.RoundingMode

object ImageMapper {
    private const val PUBLIC_GPS_SCALE = 7

    @JvmStatic
    fun toEntity(
        owner: Member,
        trip: Trip,
        imageInfo: ImageInfo,
        storedImageFile: StoredImageFile,
    ): Image = toEntity(owner, trip, null, imageInfo, storedImageFile)

    @JvmStatic
    fun toServiceResponse(image: Image): ImageServiceResponse = ImageServiceResponse(
        image.id,
        image.owner.id,
        image.trip.id,
        image.post?.id,
        image.originalFileUrl,
        image.thumbnailUrl,
        image.mimeType,
        toPublicGps(image.gpsLat),
        toPublicGps(image.gpsLng),
        image.capturedAt,
        image.deviceInfo,
        image.uploadStatus,
    )

    @JvmStatic
    fun toStoredImageFile(
        imageFileUrl: String?,
        thumbnailImageFileUrl: String?,
        fileSize: Long?,
        mimeType: String?,
    ): StoredImageFile = StoredImageFile(
        imageFileUrl,
        thumbnailImageFileUrl,
        fileSize,
        mimeType,
    )

    @JvmStatic
    fun toStoredImageFile(savedFileInfo: SavedFileInfo): StoredImageFile = toStoredImageFile(
        savedFileInfo.servingUrl,
        savedFileInfo.thumbnailUrl,
        savedFileInfo.size,
        savedFileInfo.mimeType,
    )

    @JvmStatic
    fun toUploadResponse(
        fileName: String?,
        imageServiceResponse: ImageServiceResponse?,
        message: String,
    ): ImageUploadResponse =
        if (imageServiceResponse == null) {
            ImageUploadResponse(
                fileName,
                null,
                null,
                null,
                null,
                UploadStatus.FAILED,
                message,
            )
        } else {
            ImageUploadResponse(
                fileName,
                imageServiceResponse.id,
                imageServiceResponse.originalFileUrl,
                imageServiceResponse.thumbnailUrl,
                imageServiceResponse.mimeType,
                imageServiceResponse.uploadStatus,
                "SUCCESS",
            )
        }

    @JvmStatic
    fun toUploadResponse(
        fileName: String?,
        imageServiceResponse: ImageServiceResponse?,
    ): ImageUploadResponse = toUploadResponse(fileName, imageServiceResponse, "ERROR")

    @JvmStatic
    fun toEntity(
        owner: Member,
        trip: Trip,
        post: Post?,
        imageInfo: ImageInfo,
        storedImageFile: StoredImageFile,
    ): Image {
        val originalFileUrl = requireNotNull(storedImageFile.imageFileUrl) {
            "저장된 이미지 URL은 필수입니다."
        }
        val fileSize = requireNotNull(storedImageFile.fileSize) {
            "저장된 이미지 크기는 필수입니다."
        }
        val mimeType = requireNotNull(storedImageFile.mimeType) {
            "저장된 이미지 MIME 타입은 필수입니다."
        }
        val uploadStatus = if (originalFileUrl.isBlank()) UploadStatus.FAILED else UploadStatus.STORED
        val device = imageInfo.maker?.let { maker ->
            imageInfo.model?.let { model -> "$maker - $model" }
        }
        val (latitude, longitude) = imageInfo.toGps()

        return Image(
            owner,
            trip,
            post,
            originalFileUrl,
            storedImageFile.thumbnailImageFileUrl,
            fileSize,
            mimeType,
            latitude,
            longitude,
            imageInfo.capturedAt,
            device,
            uploadStatus,
        )
    }

    @JvmStatic
    fun toImageResponse(
        imageServiceResponse: ImageServiceResponse,
    ): ImageResponse =
        ImageResponse(
            imageServiceResponse.id,
            imageServiceResponse.ownerId,
            imageServiceResponse.tripId,
            imageServiceResponse.postId,
            imageServiceResponse.originalFileUrl,
            imageServiceResponse.thumbnailUrl,
        )

    private fun ImageInfo.toGps(): Pair<BigDecimal?, BigDecimal?> {
        val latitudeValue = latitude ?: return null to null
        val longitudeValue = longitude ?: return null to null
        return BigDecimal.valueOf(latitudeValue) to BigDecimal.valueOf(longitudeValue)
    }

    private fun toPublicGps(value: BigDecimal?): BigDecimal? =
        value?.setScale(PUBLIC_GPS_SCALE, RoundingMode.FLOOR)
}
