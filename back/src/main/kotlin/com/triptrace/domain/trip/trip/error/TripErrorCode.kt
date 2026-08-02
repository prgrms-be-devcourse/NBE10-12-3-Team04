package com.triptrace.domain.trip.trip.error

import com.triptrace.global.app.Domain
import com.triptrace.global.error.DefaultErrorCode
import com.triptrace.global.error.ErrorCode

enum class TripErrorCode(
    private val code: String,
    private val message: String,
) : ErrorCode {
    MEMBER_NOT_FOUND(DefaultErrorCode.NOT_FOUND.code, "회원을 찾을 수 없습니다."),
    NOT_FOUND(DefaultErrorCode.NOT_FOUND.code, "여행기를 찾을 수 없습니다."),
    IMAGE_NOT_FOUND(DefaultErrorCode.NOT_FOUND.code, "이미지를 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN.code, "여행기에 대한 권한이 없습니다."),
    IMAGE_FORBIDDEN(DefaultErrorCode.FORBIDDEN.code, "이미지에 대한 권한이 없습니다."),
    CITY_REQUIRES_COUNTRY(DefaultErrorCode.BAD_REQUEST.code, "도시를 검색하려면 국가를 함께 지정해주세요."),
    ;

    override fun getCode(): String = code

    override fun getMessage(): String = message

    override fun getDomain(): Domain = Domain.TRIP
}
