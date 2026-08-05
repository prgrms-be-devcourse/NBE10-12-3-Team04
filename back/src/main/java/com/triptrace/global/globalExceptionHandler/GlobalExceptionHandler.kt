package com.triptrace.global.globalExceptionHandler

import com.triptrace.domain.image.image.exception.ImageProcessException
import com.triptrace.global.error.DefaultErrorCode
import com.triptrace.global.exception.ServiceException
import com.triptrace.global.rsData.RsData
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.util.NoSuchElementException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    fun handle(ex: NoSuchElementException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                DefaultErrorCode.NOT_FOUND.getCode(),
                DefaultErrorCode.NOT_FOUND.getMessage()
            ),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<RsData<Void>> {
        val message = ex.constraintViolations
            .map { violation ->
                val field = violation.propertyPath.toString().split(".", limit = 2)[1]
                val messageTemplateBits = violation.messageTemplate.split(".")
                val code = messageTemplateBits[messageTemplateBits.size - 2]

                "$field-$code-${violation.message}"
            }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData(
                DefaultErrorCode.BAD_REQUEST.getCode(),
                message
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException): ResponseEntity<RsData<Void>> {
        val msg = ex.bindingResult
            .allErrors
            .filterIsInstance<FieldError>()
            .map { error -> "${error.field}-${error.code}-${error.defaultMessage}" }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData(
                DefaultErrorCode.BAD_REQUEST.getCode(),
                msg
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                DefaultErrorCode.BAD_REQUEST.getCode(),
                DefaultErrorCode.BAD_REQUEST.getMessage()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handle(ex: MissingRequestHeaderException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                DefaultErrorCode.BAD_REQUEST.getCode(),
                "${ex.headerName}-NotBlank-${ex.localizedMessage}"
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(ServiceException::class)
    fun handle(ex: ServiceException): ResponseEntity<RsData<Void>> {
        val rsData = ex.getRsData()

        // resultCode 앞자리에서 뽑아 둔 statusCode를 그대로 HTTP 상태로 쓴다.
        return ResponseEntity(rsData, HttpStatusCode.valueOf(rsData.statusCode))
    }

    @ExceptionHandler(ImageProcessException::class)
    fun handle(ex: ImageProcessException): ResponseEntity<RsData<Void>> {
        val rsData = RsData<Void>(
            ex.resultCode,
            ex.msg,
            null
        )

        return ResponseEntity(rsData, HttpStatusCode.valueOf(rsData.statusCode))
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handle(ex: MaxUploadSizeExceededException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                DefaultErrorCode.PAYLOAD_TOO_LARGE.getCode(),
                "${DefaultErrorCode.PAYLOAD_TOO_LARGE.getMessage()}-${ex.localizedMessage}"
            ),
            HttpStatus.PAYLOAD_TOO_LARGE
        )
    }
}
