package com.triptrace.domain.image.image.processing;

import java.time.LocalDateTime;

public class ImageInfo {
    private Integer width;
    private Integer height;
    private Double longitude;
    private Double latitude;
    private LocalDateTime capturedAt;
    private String timeZone;
    private String model;
    private String maker;
    private ExifOrientation orientation;// 1 정상, 3 180도, 6 90도 시계, 8 270도 시계
    private Long fileSize;

    public ImageInfo() {
        width = height = 0;
        longitude = null;
        latitude = null;
        timeZone = null;
        model = null;
        maker = null;
        orientation = ExifOrientation.NORMAL;
        fileSize = 0L;
        capturedAt = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n------------------splitter------------------");
        sb.append("\n width: " + width)
            .append("\n height: " + height)
            .append("\n longitude: " + longitude)
            .append("\n latitude: " + latitude)
            .append("\n capturedAt: " + capturedAt)
            .append("\n timeZone: " + timeZone)
            .append("\n model: " + model)
            .append("\n maker: " + maker)
            .append("\n orientation: " + orientation)
            .append("\n fileSize: " + fileSize);
        sb.append("\n------------------splitter------------------");
        return sb.toString();
    }

    @java.lang.SuppressWarnings("all")
    public Integer getWidth() {
        return this.width;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getHeight() {
        return this.height;
    }

    @java.lang.SuppressWarnings("all")
    public Double getLongitude() {
        return this.longitude;
    }

    @java.lang.SuppressWarnings("all")
    public Double getLatitude() {
        return this.latitude;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCapturedAt() {
        return this.capturedAt;
    }

    @java.lang.SuppressWarnings("all")
    public String getTimeZone() {
        return this.timeZone;
    }

    @java.lang.SuppressWarnings("all")
    public String getModel() {
        return this.model;
    }

    @java.lang.SuppressWarnings("all")
    public String getMaker() {
        return this.maker;
    }

    @java.lang.SuppressWarnings("all")
    public ExifOrientation getOrientation() {
        return this.orientation;
    }

    @java.lang.SuppressWarnings("all")
    public Long getFileSize() {
        return this.fileSize;
    }

    @java.lang.SuppressWarnings("all")
    void setWidth(final Integer width) {
        this.width = width;
    }

    @java.lang.SuppressWarnings("all")
    void setHeight(final Integer height) {
        this.height = height;
    }

    @java.lang.SuppressWarnings("all")
    void setLongitude(final Double longitude) {
        this.longitude = longitude;
    }

    @java.lang.SuppressWarnings("all")
    void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }

    @java.lang.SuppressWarnings("all")
    void setCapturedAt(final LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    @java.lang.SuppressWarnings("all")
    void setTimeZone(final String timeZone) {
        this.timeZone = timeZone;
    }

    @java.lang.SuppressWarnings("all")
    void setModel(final String model) {
        this.model = model;
    }

    @java.lang.SuppressWarnings("all")
    void setMaker(final String maker) {
        this.maker = maker;
    }

    @java.lang.SuppressWarnings("all")
    void setOrientation(final ExifOrientation orientation) {
        this.orientation = orientation;
    }

    @java.lang.SuppressWarnings("all")
    void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }
}
