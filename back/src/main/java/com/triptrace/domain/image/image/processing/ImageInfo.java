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
