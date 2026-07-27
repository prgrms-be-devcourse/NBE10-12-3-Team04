package com.triptrace.global.app;


public enum DomainList {
    GLOBAL("01","GLOBAL"),
    AUTH("02","AUTH"),
    IMAGE("03","IMAGE"),
    MARKER("04","MARKER"),
    MEMBER("05","MEMBER"),
    POST("06","POST"),
    TRIP("07","TRIP");

    DomainList(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private final String code;
    private final String name;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
