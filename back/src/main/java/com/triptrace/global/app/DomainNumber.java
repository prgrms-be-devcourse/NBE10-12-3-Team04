package com.triptrace.global.app;

import com.triptrace.global.log.Loggable;

public enum DomainNumber implements Loggable {
    GLOBAL("01","GLOBAL"),
    AUTH("02","AUTH"),
    IMAGE("03","IMAGE"),
    MARKER("04","MARKER"),
    MEMBER("05","MEMBER"),
    POST("06","POST"),
    TRIP("07","TRIP");

    DomainNumber(String code, String message) {
        this.code = code;
        this.message = message;
    }

    private final String code;
    private final String message;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
