package com.triptrace.domain.image.image.processing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.triptrace.global.app.Domain;
import org.springframework.stereotype.Component;
import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.triptrace.domain.image.image.error.ImageErrorCode;
import com.triptrace.domain.image.image.processing.dto.ImageDateTime;
import com.triptrace.domain.image.image.processing.dto.ImageExifIF;
import com.triptrace.domain.image.image.processing.dto.ImageLocation;
import com.triptrace.domain.image.image.processing.dto.ImageWidthHeight;
import com.triptrace.domain.image.image.exception.ImageProcessException;

@Component
public class ImageMetadataExtractor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageMetadataExtractor.class);

    public ImageInfo extract(byte[] bytes) throws ImageProcessException {
        ImageInfo imageInfo = new ImageInfo();
        try (ByteArrayInputStream fis = new ByteArrayInputStream(bytes)) {
            FileType fileType = FileTypeDetector.detectFileType(fis);
            imageInfo.setFileSize(Long.valueOf(bytes.length));
            if (!fileTypeFilter(fileType)) {
                throw new ImageProcessException(ImageErrorCode.TYPE_ERROR);
            }
            Metadata metadata = ImageMetadataReader.readMetadata(fis);
            log.debug("[{}] file type: {}", Domain.IMAGE.getName(), fileType);

            ImageDateTime imageDateTime = getImageDateTime(metadata);
            if (imageDateTime != null) {
                imageInfo.setCapturedAt(imageDateTime.dateTime());
                imageInfo.setTimeZone(imageDateTime.timeZone());
            }

            ImageExifIF imageExifIF = getExifIF(metadata);
            if (imageExifIF != null) {
                imageInfo.setMaker(imageExifIF.maker());
                imageInfo.setModel(imageExifIF.device());
                imageInfo.setOrientation(imageExifIF.orientation());
            }

            ImageWidthHeight imageWidthHeight = getWidthHeight(metadata);
            if (imageWidthHeight != null) {
                imageInfo.setWidth(imageWidthHeight.width());
                imageInfo.setHeight(imageWidthHeight.height());
            }

            ImageLocation imageLocation = getLocation(metadata);
            if (imageLocation != null) {
                imageInfo.setLatitude(imageLocation.latitude());
                imageInfo.setLongitude(imageLocation.longitude());
            }
        } catch (ImageProcessingException | ImageProcessException | IOException e) {
            log.warn("[{}] image metadata parsing fallback reason: {}", Domain.IMAGE.getName(),e.getMessage());
            throw new ImageProcessException(ImageErrorCode.FILE_EXTRACT_ERROR);
        }
        return imageInfo;
    }

    private ImageDateTime getImageDateTime(Metadata metadata) {
        ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        ImageDateTime imageDateTime = null;
        if (subIFDDirectory != null) {
            String dataTimeString = subIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            String timeZone = subIFDDirectory.getDescription(ExifSubIFDDirectory.TAG_TIME_ZONE_ORIGINAL);
            LocalDateTime dateTime = null;
            try {
                if (dataTimeString != null) {
                    dateTime = LocalDateTime.parse(dataTimeString,
                        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
                }
                log.debug("[{}] date: {}, timeZone: {}",Domain.IMAGE.getName(), dateTime, timeZone);
                imageDateTime = new ImageDateTime(dateTime, timeZone);
            } catch (Exception e) {
                log.warn("[{}] date parsing fallback reason: {}",Domain.IMAGE.getName(), e.getMessage());
                return null;
            }
        } else
            log.warn("[{}] subIFD directory parsing fallback reason: null", Domain.IMAGE.getName());
        return imageDateTime;
    }

    private ImageExifIF getExifIF(Metadata metadata) {
        ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        ImageExifIF imageExifIF = null;
        ExifOrientation exifOrientation = ExifOrientation.NORMAL;
        try {
            if (exifDirectory != null) {
                int orientation = exifDirectory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                String make = exifDirectory.getDescription(ExifIFD0Directory.TAG_MAKE);
                String model = exifDirectory.getDescription(ExifIFD0Directory.TAG_MODEL);
                log.debug("[{}] orientation: {},make: {},model: {}", Domain.IMAGE.getName(),orientation, make, model);
                exifOrientation = ExifOrientation.fromExifValue(orientation);
                imageExifIF = new ImageExifIF(exifOrientation, model, make);
            } else
                log.warn("[{}] exif directory parsing fallback reason: null", Domain.IMAGE.getName());
        } catch (MetadataException e) {
            log.warn("[{}] orientation make model parsing fallback reason : {}", Domain.IMAGE.getName(),e.getMessage());
            return null;
        }
        return imageExifIF;
    }

    private ImageWidthHeight getWidthHeight(Metadata metadata) {
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        ImageWidthHeight imageWidthHeight = null;
        try {
            if (jpegDirectory != null) {
                int height = jpegDirectory.getImageHeight();
                int width = jpegDirectory.getImageWidth();
                log.debug("[{}] height: {}, width: {}",Domain.IMAGE.getName(), height, width);
                imageWidthHeight = new ImageWidthHeight(width, height);
            } else
                log.warn("[{}] jpeg directory parsing fallback reason: null", Domain.IMAGE.getName());
        } catch (MetadataException e) {
            log.warn("[{}] image size parsing fallback reason: {}", Domain.IMAGE.getName(), e.getMessage());
            return null;
        }
        return imageWidthHeight;
    }

    private ImageLocation getLocation(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        ImageLocation imageLocation = null;
        if (gpsDirectory != null) {
            try {
                double latitude = gpsDirectory.getGeoLocation().getLatitude();
                double longitude = gpsDirectory.getGeoLocation().getLongitude();
                log.debug("[{}] latitude: {},longitude: {}", Domain.IMAGE.getName(), latitude,longitude);
                imageLocation = new ImageLocation(latitude, longitude);
            } catch (Exception e) {
                log.warn("[{}] gps parsing fallback reason: {}",Domain.IMAGE.getName(), e.getMessage());
                return null;
            }
        } else
            log.warn("[{}] gps directory parsing fallback reason: null",Domain.IMAGE.getName());
        return imageLocation;
    }

    private boolean fileTypeFilter(FileType fileType) {
        log.debug("[{}] fileType: {}",Domain.IMAGE.getName(), fileType);
        return fileType == FileType.Jpeg;
    }
}


