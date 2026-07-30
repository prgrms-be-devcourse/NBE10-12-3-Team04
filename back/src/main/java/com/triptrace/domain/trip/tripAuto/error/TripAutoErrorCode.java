package com.triptrace.domain.trip.tripAuto.error;

import com.triptrace.global.app.Domain;
import com.triptrace.global.error.DefaultErrorCode;
import com.triptrace.global.error.ErrorCode;

public enum TripAutoErrorCode implements ErrorCode {
    TRIP_NOT_FOUND(DefaultErrorCode.NOT_FOUND.getCode(), "여행기를 찾을 수 없습니다."),
    FORBIDDEN(DefaultErrorCode.FORBIDDEN);

    private final String code;
    private final String message;

    TripAutoErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    TripAutoErrorCode(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
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
