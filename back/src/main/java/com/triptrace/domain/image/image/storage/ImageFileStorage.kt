package com.triptrace.domain.image.image.storage

import com.triptrace.domain.image.image.error.ImageErrorCode
import com.triptrace.domain.image.image.exception.ImageProcessException
import com.triptrace.domain.image.image.processing.ExifOrientation
import com.triptrace.domain.image.image.processing.ImageProcessor
import com.triptrace.domain.image.image.processing.dto.SavedFileInfo
import com.triptrace.domain.image.image.processing.dto.StoredFile
import com.triptrace.global.app.Domain
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Path
import java.util.UUID

@Component
class ImageFileStorage @Autowired constructor(
    properties: ImageStorageProperties,
    private val fileStorage: FileStorage,
    private val imageProcessor: ImageProcessor,
) {
    private val uploadDir = properties.upload().path()
    private val profileImagesPath = properties.upload().profilePath()
    private val servingImagesPath = properties.upload().servingPath()
    private val thumbnailImagesPath = properties.upload().thumbnailPath()
    private val thumbnailWidth = properties.thumbnail().width()
    private val thumbnailHeight = properties.thumbnail().height()
    private val jpegExt = properties.ext().jpg()

    constructor(properties: ImageStorageProperties, fileStorage: FileStorage) : this(
        properties,
        fileStorage,
        ImageProcessor(),
    )

    fun saveProfileImage(image: ByteArray?): String {
        val stored = saveImage(
            imageProcessor.read(image),
            resolveUploadPath(profileImagesPath),
            generateFileName(jpegExt),
            false,
        )
        return "$profileImagesPath/${stored.name()}"
    }

    fun saveImageWithThumbnail(image: ByteArray?, orientation: ExifOrientation?): SavedFileInfo {
        if (image == null) {
            throw ImageProcessException(ImageErrorCode.READ_ERROR)
        }
        val bufferedImage = imageProcessor.rotate(imageProcessor.read(image), orientation)
        val origin = saveImage(
            bufferedImage,
            resolveUploadPath(servingImagesPath),
            generateFileName(jpegExt),
            false,
        )
        val thumbnail = try {
            saveImage(
                bufferedImage,
                resolveUploadPath(thumbnailImagesPath),
                generateFileName(jpegExt),
                true,
            )
        } catch (exception: ImageProcessException) {
            deleteImage("$servingImagesPath/${origin.name()}")
            throw exception
        }
        return SavedFileInfo(
            "$servingImagesPath/${origin.name()}",
            "$thumbnailImagesPath/${thumbnail.name()}",
            origin.size(),
            "image/$jpegExt",
        )
    }

    fun deleteImage(imagePath: String?): Boolean {
        try {
            fileStorage.delete(resolveStoragePath(imagePath))
        } catch (exception: IOException) {
            log.warn(
                "[{}] image file storage image path: {} reason: {}",
                Domain.IMAGE.name,
                imagePath,
                exception.message,
            )
            throw ImageProcessException(ImageErrorCode.DELETE_ERROR)
        }
        return true
    }

    fun cleanUp(savedFileInfo: SavedFileInfo) {
        try {
            deleteImage(savedFileInfo.servingUrl())
        } catch (exception: ImageProcessException) {
            log.warn("[{}] image file storage fallback reason: {}", Domain.IMAGE.name, exception.message)
            throw ImageProcessException(ImageErrorCode.REWARD_TRANSACTION_ERROR)
        }

        try {
            deleteImage(savedFileInfo.thumbnailUrl())
        } catch (exception: ImageProcessException) {
            log.warn("[{}] image file storage fallback reason: {}", Domain.IMAGE.name, exception.message)
            throw ImageProcessException(ImageErrorCode.REWARD_TRANSACTION_ERROR)
        }
    }

    private fun saveImage(
        image: BufferedImage,
        directoryPath: String,
        fileName: String,
        isThumbnail: Boolean,
    ): StoredFile {
        try {
            val imageToSave = if (isThumbnail) {
                imageProcessor.resizeToFit(image, thumbnailWidth, thumbnailHeight)
            } else {
                image
            }
            return fileStorage.save(imageProcessor.encodeJpeg(imageToSave, jpegExt), directoryPath, fileName)
                ?: throw ImageProcessException(ImageErrorCode.SAVE_ERROR)
        } catch (exception: IOException) {
            log.warn("[{}] image processor fallback reason: {}", Domain.IMAGE.name, exception.message)
            throw ImageProcessException(ImageErrorCode.SAVE_ERROR)
        }
    }

    private fun generateFileName(fileExt: String) = "${UUID.randomUUID()}.$fileExt"

    private fun resolveUploadPath(path: String): String =
        Path.of(uploadDir, path.replaceFirst("^/", "")).toString()

    private fun resolveStoragePath(imagePath: String?) = resolveUploadPath(imagePath!!)

    private companion object {
        private val log = LoggerFactory.getLogger(ImageFileStorage::class.java)
    }
}
