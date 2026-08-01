package com.triptrace.domain.image.image.processing;

import com.triptrace.domain.image.image.error.ImageErrorCode;
import com.triptrace.domain.image.image.exception.ImageProcessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImageMetadataExtractorTest {

    ImageMetadataExtractor imageMetadataExtractor;

    String imageFileName = "/test-a-mail.jpg";
    byte[] imageBytes;

    @BeforeEach
    void setUp() throws IOException {
        imageMetadataExtractor = new ImageMetadataExtractor();
        try (InputStream is = getClass().getResourceAsStream(imageFileName)) {
            imageBytes = is.readAllBytes();
        }
    }

    // 성공 시나리오: 정상 이미지에서 EXIF와 이미지 정보를 추출한다.
    @Test
    @DisplayName("정상 이미지에서 메타데이터를 모두 추출한다")
    void test01() {
        ImageInfo info = imageMetadataExtractor.extract(imageBytes);

        assertThat(info).isNotNull();
        assertThat(info.getOrientation()).isNotNull();
        assertThat(info.getHeight()).isGreaterThan(0);
        assertThat(info.getWidth()).isGreaterThan(0);
        assertThat(info.getFileSize()).isGreaterThan(0);
        assertThat(info.getModel()).isNotNull();
        assertThat(info.getMaker()).isNotNull();
        assertThat(info.getTimeZone()).isNotNull();
        assertThat(info.getLatitude()).isNotNull();
        assertThat(info.getLongitude()).isNotNull();
        assertThat(info.getCapturedAt()).isNotNull();
    }

    // 성공 시나리오: 입력 바이트 배열의 파일 크기를 그대로 반환한다.
    @Test
    @DisplayName("파일 크기를 정확히 추출한다")
    void test02() {
        ImageInfo info = imageMetadataExtractor.extract(imageBytes);

        assertThat(info.getFileSize()).isEqualTo((long) imageBytes.length);
    }

    // 실패 시나리오: 이미지가 아닌 입력은 FILE_EXTRACT_ERROR로 변환해 거부한다.
    @Test
    @DisplayName("이미지 형식이 아니면 FILE_EXTRACT_ERROR 예외를 던진다")
    void extract_throwsFileExtractErrorForNonImage() {
        // given
        byte[] invalid = "not an image".getBytes();

        // when
        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageMetadataExtractor.extract(invalid)
        );

        // then
        assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.FILE_EXTRACT_ERROR));
        assertThat(exception.getMessage()).isEqualTo(ImageErrorCode.FILE_EXTRACT_ERROR.getMessage());
    }

    // 실패 시나리오: 내용이 없는 입력도 파일 형식을 판별할 수 없어 거부한다.
    @Test
    @DisplayName("빈 바이트 배열이면 FILE_EXTRACT_ERROR 예외를 던진다")
    void extract_throwsFileExtractErrorForEmptyBytes() {
        // given
        byte[] emptyBytes = new byte[0];

        // when
        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageMetadataExtractor.extract(emptyBytes)
        );

        // then
        assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.FILE_EXTRACT_ERROR));
        assertThat(exception.getMessage()).isEqualTo(ImageErrorCode.FILE_EXTRACT_ERROR.getMessage());
    }

    // given: EXIF 메타데이터가 없는 정상 JPEG 파일
    // when: 이미지 메타데이터를 추출한다.
    // then: EXIF 정보 없이도 추출 가능한 이미지 기본 정보를 반환한다.
    @Test
    @DisplayName("EXIF가 없는 JPEG에서도 이미지 기본 정보를 추출한다")
    void extract_returnsAvailableMetadataWhenExifDirectoryIsMissing() throws IOException {
        byte[] jpegWithoutExif = createJpegWithoutExif(12, 8);

        ImageInfo info = imageMetadataExtractor.extract(jpegWithoutExif);

        assertThat(info.getWidth()).isEqualTo(12);
        assertThat(info.getHeight()).isEqualTo(8);
        assertThat(info.getFileSize()).isEqualTo((long) jpegWithoutExif.length);
        assertThat(info.getMaker()).isNull();
        assertThat(info.getModel()).isNull();
        assertThat(info.getOrientation()).isEqualTo(ExifOrientation.NORMAL);
    }

    private String resultCodeOf(ImageErrorCode errorCode) {
        return "%s-%s".formatted(errorCode.getCode(), errorCode.getDomain().getCode());
    }

    private byte[] createJpegWithoutExif(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", output);
        return output.toByteArray();
    }
}
