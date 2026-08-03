package com.triptrace.domain.image.image.processing;

import com.triptrace.domain.image.image.error.ImageErrorCode;
import com.triptrace.domain.image.image.exception.ImageProcessException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class ImageProcessorTest {

    private ImageProcessor imageProcessor;
    private byte[] imageBytes;

    @BeforeEach
    void setUp() throws IOException {
        imageProcessor = new ImageProcessor();
        try (InputStream input = getClass().getResourceAsStream("/test-a-mail.jpg")) {
            imageBytes = input.readAllBytes();
        }
    }

    // read
    // given: 정상적인 Image 바이트 배열
    // when: read를 호출한다.
    // then: BufferedImage를 반환한다.
    @Test
    @DisplayName("정상 이미지 바이트 배열을 BufferedImage로 읽는다")
    void read_returnsBufferedImage() {
        BufferedImage image = imageProcessor.read(imageBytes);

        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isPositive();
        assertThat(image.getHeight()).isPositive();
    }

    // given: 정상적인 Image 바이트 배열과 IOException을 던지는 Mock ImageIO
    // when: ImageIO.read가 IOException을 던진다.
    // then: IMAGE_PROCESSING_ERROR ImageProcessException을 던진다.
    @Test
    @DisplayName("이미지 읽기 중 IOException이 발생하면 IMAGE_PROCESSING_ERROR 예외를 던진다")
    void read_throwsImageProcessingErrorWhenImageIoReadFails() throws IOException {
        try (MockedStatic<ImageIO> imageIO = mockStatic(ImageIO.class)) {
            imageIO.when(() -> ImageIO.read(any(InputStream.class)))
                .thenThrow(new IOException("이미지를 읽을 수 없습니다."));

            ImageProcessException exception = assertThrows(
                ImageProcessException.class,
                () -> imageProcessor.read(imageBytes)
            );

            assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.IMAGE_PROCESSING_ERROR));
        }
    }

    // rotate
    // given: exifOrientation이 NORMAL인 이미지
    // when: rotate를 호출한다.
    // then: 이미지 인스턴스를 그대로 반환한다.
    @Test
    @DisplayName("NORMAL 방향의 이미지는 회전하지 않는다")
    void rotate_returnsSameImageWhenOrientationIsNormal() {
        BufferedImage image = createColorImage();

        BufferedImage rotated = imageProcessor.rotate(image, ExifOrientation.NORMAL);

        assertThat(rotated).isSameAs(image);
    }

    // given: 정상적인 BufferedImage와 ROTATE_180 방향
    // when: rotate를 호출한다.
    // then: 180도 회전한 이미지를 반환한다.
    // 색 구성을 비교하여 회전 되었는지 확인한다.
    @Test
    @DisplayName("ROTATE_180 방향이면 이미지를 180도 회전한다")
    void rotate_rotatesImage180Degrees() {
        BufferedImage rotated = imageProcessor.rotate(createColorImage(), ExifOrientation.ROTATE_180);

        assertThat(rotated.getWidth()).isEqualTo(2);
        assertThat(rotated.getHeight()).isEqualTo(3);
        assertThat(rotated.getRGB(1, 2)).isEqualTo(Color.RED.getRGB());
        assertThat(rotated.getRGB(0, 0)).isEqualTo(Color.WHITE.getRGB());
    }

    // given: 정상적인 BufferedImage와 ROTATE_90_CW 방향
    // when: rotate를 호출한다.
    // then: 시계 방향으로 90도 회전한 이미지를 반환한다.
    @Test
    @DisplayName("ROTATE_90_CW 방향이면 이미지를 시계 방향으로 90도 회전한다")
    void rotate_rotatesImage90DegreesClockwise() {
        BufferedImage rotated = imageProcessor.rotate(createColorImage(), ExifOrientation.ROTATE_90_CW);

        assertThat(rotated.getWidth()).isEqualTo(3);
        assertThat(rotated.getHeight()).isEqualTo(2);
        assertThat(rotated.getRGB(0, 0)).isEqualTo(Color.BLACK.getRGB());
        assertThat(rotated.getRGB(2, 1)).isEqualTo(Color.GREEN.getRGB());
    }

    // given: 정상적인 BufferedImage와 ROTATE_270_CW 방향
    // when: rotate를 호출한다.
    // then: 시계 방향으로 270도 회전한 이미지를 반환한다.
    @Test
    @DisplayName("ROTATE_270_CW 방향이면 이미지를 시계 방향으로 270도 회전한다")
    void rotate_rotatesImage270DegreesClockwise() {
        BufferedImage rotated = imageProcessor.rotate(createColorImage(), ExifOrientation.ROTATE_270_CW);

        assertThat(rotated.getWidth()).isEqualTo(3);
        assertThat(rotated.getHeight()).isEqualTo(2);
        assertThat(rotated.getRGB(0, 0)).isEqualTo(Color.GREEN.getRGB());
        assertThat(rotated.getRGB(2, 1)).isEqualTo(Color.BLACK.getRGB());
    }

    // resizeToFit
    // given: 목표 너비보다 넓은 가로 이미지
    // when: 목표 영역에 맞게 크기를 조절한다.
    // then: 비율을 유지하며 너비를 기준으로 축소한다.
    @Test
    @DisplayName("가로 이미지를 비율을 유지해 목표 너비로 축소한다")
    void resizeToFit_scalesLandscapeImageByWidth() {
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);

        BufferedImage resized = imageProcessor.resizeToFit(image, 100, 100);

        assertThat(resized.getWidth()).isEqualTo(100);
        assertThat(resized.getHeight()).isEqualTo(50);
    }

    // given: 목표 높이보다 높은 세로 이미지
    // when: 목표 영역에 맞게 크기를 조절한다.
    // then: 비율을 유지하며 높이를 기준으로 축소한다.
    @Test
    @DisplayName("세로 이미지를 비율을 유지해 목표 높이로 축소한다")
    void resizeToFit_scalesPortraitImageByHeight() {
        BufferedImage image = new BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB);

        BufferedImage resized = imageProcessor.resizeToFit(image, 100, 100);

        assertThat(resized.getWidth()).isEqualTo(50);
        assertThat(resized.getHeight()).isEqualTo(100);
    }

    // given: 목표 영역보다 작은 이미지
    // when: 목표 영역에 맞게 크기를 조절한다.
    // then: 이미지를 확대하지 않고 원래 크기를 유지한다.
    @Test
    @DisplayName("작은 이미지는 목표 영역이 더 커도 확대하지 않는다")
    void resizeToFit_doesNotUpscaleSmallerImage() {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);

        BufferedImage resized = imageProcessor.resizeToFit(image, 100, 100);

        assertThat(resized.getWidth()).isEqualTo(40);
        assertThat(resized.getHeight()).isEqualTo(20);
    }

    // encodeJpeg
    // given: 정상적인 BufferedImage와 jpeg 확장자
    // when: encodeJpeg를 호출한다.
    // then: 읽을 수 있는 JPEG 바이트 배열을 반환한다.
    @Test
    @DisplayName("이미지를 JPEG 바이트 배열로 인코딩한다")
    void encodeJpeg_returnsReadableJpegBytes() throws IOException {
        byte[] encoded = imageProcessor.encodeJpeg(createColorImage(), "jpeg");

        assertThat(encoded).isNotEmpty();
        assertThat(ImageIO.read(new ByteArrayInputStream(encoded))).isNotNull();
    }

    // given: null 이미지와 정상적인 jpeg 확장자
    // when: encodeJpeg가 convertToRGB를 실행한다.
    // then: IMAGE_PROCESSING_ERROR ImageProcessException을 던진다.
    @Test
    @DisplayName("null 이미지를 JPEG로 인코딩하면 IMAGE_PROCESSING_ERROR 예외를 던진다")
    void encodeJpeg_throwsImageProcessingErrorWhenImageIsNull() {
        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageProcessor.encodeJpeg(null, "jpeg")
        );

        assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.IMAGE_PROCESSING_ERROR));
    }

    // given: 정상적인 BufferedImage와 false를 반환하는 Mock ImageIO
    // when: ImageIO.write가 false를 반환한다.
    // then: SAVE_ERROR ImageProcessException을 던진다.
    @Test
    @DisplayName("JPEG 작성 결과가 false이면 SAVE_ERROR 예외를 던진다")
    void encodeJpeg_throwsSaveErrorWhenImageIoWriteReturnsFalse() throws IOException {
        try (MockedStatic<ImageIO> imageIO = mockStatic(ImageIO.class)) {
            imageIO.when(() -> ImageIO.write(any(BufferedImage.class), anyString(), any(OutputStream.class)))
                .thenReturn(false);

            ImageProcessException exception = assertThrows(
                ImageProcessException.class,
                () -> imageProcessor.encodeJpeg(createColorImage(), "jpeg")
            );

            assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.SAVE_ERROR));
        }
    }

    // given: 정상적인 BufferedImage와 IOException을 던지는 Mock ImageIO
    // when: ImageIO.write가 IOException을 던진다.
    // then: SAVE_ERROR ImageProcessException을 던진다.
    @Test
    @DisplayName("JPEG 작성 중 IOException이 발생하면 SAVE_ERROR 예외를 던진다")
    void encodeJpeg_throwsSaveErrorWhenImageIoWriteFails() throws IOException {
        try (MockedStatic<ImageIO> imageIO = mockStatic(ImageIO.class)) {
            imageIO.when(() -> ImageIO.write(any(BufferedImage.class), anyString(), any(OutputStream.class)))
                .thenThrow(new IOException("이미지를 저장할 수 없습니다."));

            ImageProcessException exception = assertThrows(
                ImageProcessException.class,
                () -> imageProcessor.encodeJpeg(createColorImage(), "jpeg")
            );

            assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.SAVE_ERROR));
        }
    }

    private BufferedImage createColorImage() {
        BufferedImage image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        image.setRGB(1, 0, Color.GREEN.getRGB());
        image.setRGB(0, 1, Color.BLUE.getRGB());
        image.setRGB(1, 1, Color.YELLOW.getRGB());
        image.setRGB(0, 2, Color.BLACK.getRGB());
        image.setRGB(1, 2, Color.WHITE.getRGB());
        return image;
    }

    private String resultCodeOf(ImageErrorCode errorCode) {
        return "%s-%s".formatted(errorCode.getCode(), errorCode.getDomain().getCode());
    }
}
