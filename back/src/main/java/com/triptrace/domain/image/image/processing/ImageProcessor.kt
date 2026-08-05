package com.triptrace.domain.image.image.processing

import com.triptrace.domain.image.image.error.ImageErrorCode
import com.triptrace.domain.image.image.exception.ImageProcessException
import com.triptrace.global.app.Domain
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

@Component
class ImageProcessor {
    fun read(image: ByteArray?): BufferedImage {
        if (image == null) {
            throw ImageProcessException(ImageErrorCode.IMAGE_PROCESSING_ERROR)
        }

        try {
            return ImageIO.read(ByteArrayInputStream(image))
                ?: throw ImageProcessException(ImageErrorCode.READ_ERROR)
        } catch (exception: IOException) {
            log.warn(
                "[{}] image processor fallback reason: {}",
                Domain.IMAGE.name,
                exception.message,
            )
            throw ImageProcessException(ImageErrorCode.IMAGE_PROCESSING_ERROR)
        }
    }

    fun rotate(image: BufferedImage, exifOrientation: ExifOrientation?): BufferedImage {
        if (exifOrientation == null || exifOrientation == ExifOrientation.NORMAL) {
            return image
        }

        val transform = AffineTransform()
        when (exifOrientation) {
            ExifOrientation.ROTATE_180 -> {
                transform.translate(image.width.toDouble(), image.height.toDouble())
                transform.rotate(Math.PI)
            }

            ExifOrientation.ROTATE_90_CW -> {
                transform.translate(image.height.toDouble(), 0.0)
                transform.rotate(Math.PI / 2)
            }

            ExifOrientation.ROTATE_270_CW -> {
                transform.translate(0.0, image.width.toDouble())
                transform.rotate(-Math.PI / 2)
            }

            ExifOrientation.NORMAL -> Unit
        }

        return AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR)
            .filter(image, null)
    }

    fun resizeToFit(image: BufferedImage, newWidth: Int, newHeight: Int): BufferedImage {
        val ratio = minOf(
            minOf(newWidth.toDouble() / image.width, newHeight.toDouble() / image.height),
            1.0,
        )
        val scaleHeight = (image.height * ratio).toInt()
        val scaleWidth = (image.width * ratio).toInt()
        val resized = BufferedImage(scaleWidth, scaleHeight, image.type)
        val graphics = resized.createGraphics()
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        graphics.drawImage(image, 0, 0, scaleWidth, scaleHeight, null)
        graphics.dispose()
        return resized
    }

    fun encodeJpeg(image: BufferedImage?, jpegExt: String): ByteArray {
        val rgbImage = convertToRgb(image)
        try {
            val bytes = ByteArrayOutputStream()
            if (!ImageIO.write(rgbImage, jpegExt, bytes)) {
                throw ImageProcessException(ImageErrorCode.SAVE_ERROR)
            }
            return bytes.toByteArray()
        } catch (exception: IOException) {
            log.warn(
                "[{}] image processor fallback reason: {}",
                Domain.IMAGE.name,
                exception.message,
            )
            throw ImageProcessException(ImageErrorCode.SAVE_ERROR)
        }
    }

    private fun convertToRgb(image: BufferedImage?): BufferedImage {
        if (image == null) {
            throw ImageProcessException(ImageErrorCode.IMAGE_PROCESSING_ERROR)
        }
        if (!image.colorModel.hasAlpha()) {
            return image
        }

        val rgbImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val graphics: Graphics2D = rgbImage.createGraphics()
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        return rgbImage
    }

    private companion object {
        private val log = LoggerFactory.getLogger(ImageProcessor::class.java)
    }
}
