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

        sb.append(" width: " + width)
            .append(" height: " + height)
            .append(" longitude: " + longitude)
            .append(" latitude: " + latitude)
            .append(" capturedAt: " + capturedAt)
            .append(" timeZone: " + timeZone)
            .append(" model: " + model)
            .append(" maker: " + maker)
            .append(" orientation: " + orientation)
            .append(" fileSize: " + fileSize);

        return sb.toString();
    }

    public Integer getWidth() {
        return this.width;
    }

    public Integer getHeight() {
        return this.height;
    }

    public Double getLongitude() {
        return this.longitude;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public LocalDateTime getCapturedAt() {
        return this.capturedAt;
    }

    public String getTimeZone() {
        return this.timeZone;
    }

    public String getModel() {
        return this.model;
    }

    public String getMaker() {
        return this.maker;
    }

    public ExifOrientation getOrientation() {
        return this.orientation;
    }

    public Long getFileSize() {
        return this.fileSize;
    }

    void setWidth(final Integer width) {
        this.width = width;
    }

    void setHeight(final Integer height) {
        this.height = height;
    }

    void setLongitude(final Double longitude) {
        this.longitude = longitude;
    }

    void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }

    void setCapturedAt(final LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    void setTimeZone(final String timeZone) {
        this.timeZone = timeZone;
    }

    void setModel(final String model) {
        this.model = model;
    }

    void setMaker(final String maker) {
        this.maker = maker;
    }

    void setOrientation(final ExifOrientation orientation) {
        this.orientation = orientation;
    }

    void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }
}
