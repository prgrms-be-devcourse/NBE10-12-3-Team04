package com.triptrace.domain.image.image.processing

import com.drew.imaging.FileType
import com.drew.imaging.FileTypeDetector
import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Metadata
import com.drew.metadata.MetadataException
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.jpeg.JpegDirectory
import com.triptrace.domain.image.image.error.ImageErrorCode
import com.triptrace.domain.image.image.exception.ImageProcessException
import com.triptrace.domain.image.image.processing.dto.ImageDateTime
import com.triptrace.domain.image.image.processing.dto.ImageExifIF
import com.triptrace.domain.image.image.processing.dto.ImageLocation
import com.triptrace.domain.image.image.processing.dto.ImageWidthHeight
import com.triptrace.global.app.Domain
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ImageMetadataExtractor {
    fun extract(bytes: ByteArray): ImageInfo {
        return try {
            ByteArrayInputStream(bytes).use { input ->
                val fileType = FileTypeDetector.detectFileType(input)
                if (!fileTypeFilter(fileType)) {
                    throw ImageProcessException(ImageErrorCode.TYPE_ERROR)
                }

                val metadata = ImageMetadataReader.readMetadata(input)
                log.debug("[{}] file type: {}", Domain.IMAGE.name, fileType)
                val dateTime = getImageDateTime(metadata)
                val exif = getExifIf(metadata)
                val size = getWidthHeight(metadata)
                val location = getLocation(metadata)

                ImageInfo(
                    width = size?.width,
                    height = size?.height,
                    longitude = location?.longitude,
                    latitude = location?.latitude,
                    capturedAt = dateTime?.dateTime,
                    timeZone = dateTime?.timeZone,
                    model = exif?.device,
                    maker = exif?.maker,
                    orientation = exif?.orientation ?: ExifOrientation.NORMAL,
                    fileSize = bytes.size.toLong(),
                )
            }
        } catch (exception: ImageProcessingException) {
            throwExtractException(exception)
        } catch (exception: ImageProcessException) {
            throwExtractException(exception)
        } catch (exception: IOException) {
            throwExtractException(exception)
        }
    }

    private fun getImageDateTime(metadata: Metadata): ImageDateTime? {
        val directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
            ?: run {
                log.warn("[{}] subIFD directory parsing fallback reason: null", Domain.IMAGE.name)
                return null
            }

        return try {
            val dateTimeString = directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
            val timeZone = directory.getDescription(ExifSubIFDDirectory.TAG_TIME_ZONE_ORIGINAL)
            val dateTime = dateTimeString?.let {
                LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
            }
            log.debug("[{}] date: {}, timeZone: {}", Domain.IMAGE.name, dateTime, timeZone)
            ImageDateTime(dateTime, timeZone)
        } catch (exception: Exception) {
            log.warn("[{}] date parsing fallback reason: {}", Domain.IMAGE.name, exception.message)
            null
        }
    }

    private fun getExifIf(metadata: Metadata): ImageExifIF? {
        val directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            ?: run {
                log.warn("[{}] exif directory parsing fallback reason: null", Domain.IMAGE.name)
                return null
            }

        return try {
            val orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION)
            val make = directory.getDescription(ExifIFD0Directory.TAG_MAKE)
            val model = directory.getDescription(ExifIFD0Directory.TAG_MODEL)
            log.debug(
                "[{}] orientation: {},make: {},model: {}",
                Domain.IMAGE.name,
                orientation,
                make,
                model,
            )
            ImageExifIF(ExifOrientation.fromExifValue(orientation), model, make)
        } catch (exception: MetadataException) {
            log.warn(
                "[{}] orientation make model parsing fallback reason : {}",
                Domain.IMAGE.name,
                exception.message,
            )
            null
        }
    }

    private fun getWidthHeight(metadata: Metadata): ImageWidthHeight? {
        val directory = metadata.getFirstDirectoryOfType(JpegDirectory::class.java)
            ?: run {
                log.warn("[{}] jpeg directory parsing fallback reason: null", Domain.IMAGE.name)
                return null
            }

        return try {
            val height = directory.imageHeight
            val width = directory.imageWidth
            log.debug("[{}] height: {}, width: {}", Domain.IMAGE.name, height, width)
            ImageWidthHeight(width, height)
        } catch (exception: MetadataException) {
            log.warn(
                "[{}] image size parsing fallback reason: {}",
                Domain.IMAGE.name,
                exception.message,
            )
            null
        }
    }

    private fun getLocation(metadata: Metadata): ImageLocation? {
        val directory = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
            ?: run {
                log.warn("[{}] gps directory parsing fallback reason: null", Domain.IMAGE.name)
                return null
            }

        return try {
            val location = directory.geoLocation
            val latitude = location.latitude
            val longitude = location.longitude
            log.debug("[{}] latitude: {},longitude: {}", Domain.IMAGE.name, latitude, longitude)
            ImageLocation(latitude, longitude)
        } catch (exception: Exception) {
            log.warn("[{}] gps parsing fallback reason: {}", Domain.IMAGE.name, exception.message)
            null
        }
    }

    private fun fileTypeFilter(fileType: FileType): Boolean {
        log.debug("[{}] fileType: {}", Domain.IMAGE.name, fileType)
        return fileType == FileType.Jpeg
    }

    private fun throwExtractException(exception: Exception): Nothing {
        log.warn(
            "[{}] image metadata parsing fallback reason: {}",
            Domain.IMAGE.name,
            exception.message,
        )
        throw ImageProcessException(ImageErrorCode.FILE_EXTRACT_ERROR)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(ImageMetadataExtractor::class.java)
    }
}
