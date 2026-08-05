package com.triptrace.domain.image.image.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.triptrace.domain.image.image.error.ImageErrorCode;
import com.triptrace.domain.image.image.processing.ExifOrientation;
import com.triptrace.domain.image.image.processing.dto.SavedFileInfo;
import com.triptrace.domain.image.image.processing.dto.StoredFile;
import com.triptrace.domain.image.image.exception.ImageProcessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImageFileStorageTest {
    //LocalFileStorage save delete
    @TempDir
    Path tempDir;

    ImageFileStorage imageFileStorage;

    String imageFileName = "/test-a-mail.jpg";
    byte[] imageBytes;

    @BeforeEach
    void setUp() throws IOException {
        ImageStorageProperties properties = new ImageStorageProperties(
            new ImageStorageProperties.Upload(
                tempDir.toString(),
                "/images/serving",
                "/images/thumbnail",
                "/images/profile",
                "/"
            ),
            new ImageStorageProperties.Thumbnail(1024, 1024),
            new ImageStorageProperties.Ext("jpeg")
        );
        imageFileStorage = new ImageFileStorage(properties, new LocalFileStorage());

        try (InputStream is = getClass().getResourceAsStream(imageFileName)) {
            imageBytes = is.readAllBytes();
        }
    }

    @Test
    @DisplayName("원본과 섬네일을 저장하면 각각의 URL과 파일 크기를 반환한다")
    void test01() {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);

        assertThat(savedFileInfo).isNotNull();
        assertThat(savedFileInfo.getServingUrl()).startsWith("/images/serving/");
        assertThat(savedFileInfo.getThumbnailUrl()).startsWith("/images/thumbnail/");
        assertThat(savedFileInfo.getSize()).isGreaterThan(0);
    }

    @Test
    @DisplayName("저장된 원본과 섬네일 파일이 실제 디스크에 생성된다")
    void test02() {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);

        assertThat(Files.exists(diskPath(savedFileInfo.getServingUrl()))).isTrue();
        assertThat(Files.exists(diskPath(savedFileInfo.getThumbnailUrl()))).isTrue();
    }

    @Test
    @DisplayName("섬네일은 지정한 크기 이하로 리사이즈된다")
    void test03() throws IOException {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);

        BufferedImage thumbnail = ImageIO.read(diskPath(savedFileInfo.getThumbnailUrl()).toFile());

        assertThat(thumbnail.getWidth()).isLessThanOrEqualTo(1024);
        assertThat(thumbnail.getHeight()).isLessThanOrEqualTo(1024);
    }

    @Test
    @DisplayName("90도 회전을 적용하면 가로/세로가 뒤바뀐 크기로 저장된다")
    void test04() throws IOException {
        SavedFileInfo normal = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);
        SavedFileInfo rotated = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.ROTATE_90_CW);

        BufferedImage normalImage = ImageIO.read(diskPath(normal.getServingUrl()).toFile());
        BufferedImage rotatedImage = ImageIO.read(diskPath(rotated.getServingUrl()).toFile());

        assertThat(rotatedImage.getWidth()).isEqualTo(normalImage.getHeight());
        assertThat(rotatedImage.getHeight()).isEqualTo(normalImage.getWidth());
    }

    @Test
    @DisplayName("프로필 이미지를 저장하면 URL을 반환하고 실제 파일도 생성된다")
    void test05() {
        String url = imageFileStorage.saveProfileImage(imageBytes);

        assertThat(url).startsWith("/images/profile/");
        assertThat(Files.exists(diskPath(url))).isTrue();
    }

    @Test
    @DisplayName("이미지를 삭제하면 디스크에서 파일이 사라진다")
    void test06() {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);
        Path originFile = diskPath(savedFileInfo.getServingUrl());
        assertThat(Files.exists(originFile)).isTrue();

        imageFileStorage.deleteImage(savedFileInfo.getServingUrl());

        assertThat(Files.exists(originFile)).isFalse();
    }

    @Test
    @DisplayName("null 이미지를 저장하려 하면 예외를 던진다")
    void test07() {
        assertThatThrownBy(() -> imageFileStorage.saveImageWithThumbnail(null, ExifOrientation.NORMAL))
            .isInstanceOf(ImageProcessException.class);
    }

    @Test
    @DisplayName("이미지가 아닌 데이터를 저장하려 하면 예외를 던진다")
    void test08() {
        byte[] invalid = "not an image".getBytes();

        assertThatThrownBy(() -> imageFileStorage.saveImageWithThumbnail(invalid, ExifOrientation.NORMAL))
            .isInstanceOf(ImageProcessException.class);
    }

    @Test
    @DisplayName("null 이미지를 저장하려 하면 READ_ERROR 예외를 던진다.")
    void test09() {
        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageFileStorage.saveImageWithThumbnail(null, ExifOrientation.NORMAL)
        );
        assertThat(exception.getResultCode())
            .isEqualTo(
                "%s-%s".formatted(
                    ImageErrorCode.READ_ERROR.getCode(),
                    ImageErrorCode.READ_ERROR.getDomain().getCode()
                ));
        assertThat(exception.getMessage()).isEqualTo(ImageErrorCode.READ_ERROR.getMessage());
    }

    //saveImage
    // IOException은 실제 디스크 오류를 만들지 않고 FileStorage mock으로 재현한다.
    // given: 파일 저장소가 파일 저장 중 IOException을 던진다.
    // when: 원본과 섬네일 저장을 요청한다.
    // then: 저장 계층의 IOException을 SAVE_ERROR ImageProcessException으로 변환한다.
    @Test
    @DisplayName("파일 저장 중 IOException이 발생하면 SAVE_ERROR 예외를 던진다")
    void saveImageWithThumbnail_throwsSaveErrorWhenFileStorageSaveFails() throws IOException {
        FileStorage failingFileStorage = mock(FileStorage.class);

        when(failingFileStorage.save(any(byte[].class), anyString(), anyString()))
            .thenThrow(new IOException("디스크에 파일을 쓸 수 없습니다."));

        imageFileStorage = new ImageFileStorage(storageProperties(), failingFileStorage);

        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL)
        );

        assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.SAVE_ERROR));
        assertThat(exception.getMessage()).isEqualTo(ImageErrorCode.SAVE_ERROR.getMessage());
    }

    //saveImageWithThumbnail
    // given: 원본 저장은 성공하지만 섬네일 저장에서 IOException이 발생한다.
    // when: 원본과 섬네일 저장을 요청한다.
    // then: 이미 저장된 원본 파일을 삭제하는 보상 동작을 수행하고 SAVE_ERROR를 던진다.
    @Test
    @DisplayName("섬네일 저장 실패 시 원본 파일을 보상 삭제한다")
    void saveImageWithThumbnail_deletesOriginWhenThumbnailSaveFails() throws IOException {
        FileStorage fileStorage = mock(FileStorage.class);
        when(fileStorage.save(any(byte[].class), anyString(), anyString()))
            .thenReturn(new StoredFile("/images/serving", "origin.jpg", 1024L))
            .thenThrow(new IOException("섬네일을 저장할 수 없습니다."));
        imageFileStorage = new ImageFileStorage(storageProperties(), fileStorage);

        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL)
        );

        assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.SAVE_ERROR));
        verify(fileStorage).delete(tempDir.resolve("images/serving/origin.jpg").toString());
    }

    //deleteImage
    // given: 파일 저장소가 삭제 중 IOException을 던진다.
    // when: 이미지를 삭제한다.
    // then: 삭제 계층의 IOException을 DELETE_ERROR ImageProcessException으로 변환한다.
    @Test
    @DisplayName("파일 삭제 중 IOException이 발생하면 DELETE_ERROR 예외를 던진다")
    void deleteImage_throwsDeleteErrorWhenFileStorageDeleteFails() throws IOException {
        FileStorage failingFileStorage = mock(FileStorage.class);
        doThrow(new IOException("파일을 삭제할 수 없습니다."))
            .when(failingFileStorage)
            .delete(anyString());
        imageFileStorage = new ImageFileStorage(storageProperties(), failingFileStorage);

        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageFileStorage.deleteImage("/images/serving/image.jpg")
        );

        assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.DELETE_ERROR));
        assertThat(exception.getMessage()).isEqualTo(ImageErrorCode.DELETE_ERROR.getMessage());
    }

    // cleanUp
    // given: 저장된 원본 파일과 섬네일 파일이 있다.
    // when: 저장 결과로 cleanUp을 호출한다.
    // then: 원본과 섬네일 파일을 모두 삭제한다.
    @Test
    @DisplayName("원본과 섬네일 파일을 함께 정리한다")
    void cleanUp_deletesOriginAndThumbnail() {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);
        Path originFile = diskPath(savedFileInfo.getServingUrl());
        Path thumbnailFile = diskPath(savedFileInfo.getThumbnailUrl());

        imageFileStorage.cleanUp(savedFileInfo);

        assertThat(Files.exists(originFile)).isFalse();
        assertThat(Files.exists(thumbnailFile)).isFalse();
    }

    // given: 원본 파일 삭제 중 IOException이 발생한다.
    // when: 저장된 원본·섬네일 파일을 정리한다.
    // then: 보상 트랜잭션 실패를 나타내는 REWARD_TRANSACTION_ERROR를 던진다.
    @Test
    @DisplayName("원본 파일 정리 실패 시 REWARD_TRANSACTION_ERROR 예외를 던진다")
    void cleanUp_throwsRewardTransactionErrorWhenOriginDeleteFails() throws IOException {
        FileStorage failingFileStorage = mock(FileStorage.class);
        doThrow(new IOException(ImageErrorCode.DELETE_ERROR.getMessage()))
            .when(failingFileStorage)
            .delete(anyString());
        imageFileStorage = new ImageFileStorage(storageProperties(), failingFileStorage);

        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageFileStorage.cleanUp(
                new SavedFileInfo("servingImage", "thumbnailImage", 1024L, "image/jpeg")
            )
        );

        assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.REWARD_TRANSACTION_ERROR));
        assertThat(exception.getMessage()).isEqualTo(ImageErrorCode.REWARD_TRANSACTION_ERROR.getMessage());
    }

    // given: 원본 파일 삭제는 성공하고, 섬네일 파일 삭제에서 IOException이 발생한다.
    // when: 저장된 원본·섬네일 파일을 정리한다.
    // then: 두 번째 삭제 실패를 REWARD_TRANSACTION_ERROR로 변환한다.
    @Test
    @DisplayName("섬네일 파일 정리 실패 시 REWARD_TRANSACTION_ERROR 예외를 던진다")
    void cleanUp_throwsRewardTransactionErrorWhenThumbnailDeleteFails() throws IOException {
        FileStorage fileStorage = mock(FileStorage.class);
        doNothing()
            .doThrow(new IOException(ImageErrorCode.DELETE_ERROR.getMessage()))
            .when(fileStorage)
            .delete(anyString());
        imageFileStorage = new ImageFileStorage(storageProperties(), fileStorage);

        ImageProcessException exception = assertThrows(
            ImageProcessException.class,
            () -> imageFileStorage.cleanUp(
                new SavedFileInfo("servingImage", "thumbnailImage", 1024L, "image/jpeg")
            )
        );

        assertThat(exception.getResultCode()).isEqualTo(resultCodeOf(ImageErrorCode.REWARD_TRANSACTION_ERROR));
        assertThat(exception.getMessage()).isEqualTo(ImageErrorCode.REWARD_TRANSACTION_ERROR.getMessage());
        verify(fileStorage).delete(tempDir.resolve("servingImage").toString());
        verify(fileStorage).delete(tempDir.resolve("thumbnailImage").toString());
    }

    private ImageStorageProperties storageProperties() {
        return new ImageStorageProperties(
            new ImageStorageProperties.Upload(
                tempDir.toString(), "/images/serving", "/images/thumbnail", "/images/profile", "/"
            ),
            new ImageStorageProperties.Thumbnail(1024, 1024),
            new ImageStorageProperties.Ext("jpeg")
        );
    }


    private String resultCodeOf(ImageErrorCode errorCode) {
        return "%s-%s".formatted(errorCode.getCode(), errorCode.getDomain().getCode());
    }

    private Path diskPath(String storedUrl) {
        return Paths.get(tempDir.toString() + storedUrl);
    }
}
