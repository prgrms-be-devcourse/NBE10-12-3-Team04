package com.triptrace.domain.trip.trip.error

import com.triptrace.global.app.Domain
import com.triptrace.global.error.DefaultErrorCode
import com.triptrace.global.error.ErrorCode

enum class TripErrorCode(code: String, message: String) : ErrorCode {
    MEMBER_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "회원을 찾을 수 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "여행기를 찾을 수 없습니다."),
    IMAGE_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "이미지를 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN.getCode(), "여행기에 대한 권한이 없습니다."),
    IMAGE_FORBIDDEN(DefaultErrorCode.FORBIDDEN.getCode(), "이미지에 대한 권한이 없습니다."),
    CITY_REQUIRES_COUNTRY(DefaultErrorCode.BAD_REQUEST.getCode(), "도시를 검색하려면 국가를 함께 지정해주세요.");

    private val code: String?
    private val message: String?

    init {
        this.code = code
        this.message = message
    }

    override fun getCode(): String? {
        return code
    }

    override fun getMessage(): String? {
        return message
    }

    override fun getDomain(): Domain {
        return Domain.TRIP
    }
}
