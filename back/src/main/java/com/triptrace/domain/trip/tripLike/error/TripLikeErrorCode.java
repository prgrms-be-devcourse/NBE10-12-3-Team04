package com.triptrace.domain.trip.tripLike.error;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.DefaultErrorCode;
import com.triptrace.global.error.ErrorCode;

public enum TripLikeErrorCode implements ErrorCode {
    ALREADY_LIKED(DefaultErrorCode.CONFLICT.getCode(), "이미 좋아요한 여행기입니다."),
    NOT_LIKED(DefaultErrorCode.CONFLICT.getCode(), "좋아요한 적이 없는 여행기입니다."),
    MEMBER_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "회원을 찾을 수 없습니다."),
    TRIP_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "여행기를 찾을 수 없습니다.");

    private final String code;
    private final String message;

    TripLikeErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Domain getDomain() {
        return Domain.TRIP;
    }
}
