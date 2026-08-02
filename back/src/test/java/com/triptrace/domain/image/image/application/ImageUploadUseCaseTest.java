package com.triptrace.domain.image.image.application;

import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.response.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.error.ImageErrorCode;
import com.triptrace.domain.image.image.exception.ImageProcessException;
import com.triptrace.domain.image.image.processing.ImageInfo;
import com.triptrace.domain.image.image.processing.ImageMetadataExtractor;
import com.triptrace.domain.image.image.processing.dto.SavedFileInfo;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.image.image.storage.ImageFileStorage;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.exception.ServiceException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageUploadUseCaseTest {

    // given: 소유자의 여행과 정상 이미지 파일
    // when: 게시글 없이 이미지를 업로드한다.
    // then: 저장된 이미지 응답을 반환한다.
    @Test
    @DisplayName("게시글 없이 이미지를 업로드하면 저장 응답을 반환한다")
    void uploadImages_returnsStoredResponse() {
        Fixture fixture = new Fixture();
        fixture.stubSuccessfulStorageAndCreate();

        List<ImageUploadResponse> responses = fixture.useCase.uploadImages(1L, 2L, new MultipartFile[] {imageFile()});

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);
        assertThat(responses.getFirst().message()).isEqualTo("SUCCESS");
        verify(fixture.imageService).create(any());
    }

    // given: 소유자 여행에 속한 게시글과 정상 이미지 파일
    // when: 게시글 ID와 함께 이미지를 업로드한다.
    // then: 게시글이 연결된 Image를 생성하고 저장 응답을 반환한다.
    @Test
    @DisplayName("게시글 ID와 함께 업로드하면 이미지에 게시글을 연결한다")
    void uploadImages_withPost_connectsPostToCreatedImage() {
        Fixture fixture = new Fixture();
        fixture.stubSuccessfulStorageAndCreate();

        List<ImageUploadResponse> responses = fixture.useCase.uploadImages(
            1L, 2L, 3L, new MultipartFile[] {imageFile()}
        );

        ArgumentCaptor<Image> imageCaptor = ArgumentCaptor.forClass(Image.class);
        verify(fixture.imageService).create(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getPost()).isSameAs(fixture.post);
        assertThat(responses.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);
    }

    // given: 빈 배열 또는 null 이미지 배열
    // when: 업로드를 요청한다.
    // then: NO_IMAGE ServiceException을 던진다.
    @Test
    @DisplayName("빈 이미지 배열 업로드 요청은 NO_IMAGE 예외를 던진다")
    void uploadImages_throwsNoImageWhenImagesAreEmpty() {
        Fixture fixture = new Fixture();

        ServiceException exception = assertThrows(
            ServiceException.class,
            () -> fixture.useCase.uploadImages(1L, 2L, new MultipartFile[0])
        );

        assertThat(exception.getRsData().resultCode()).isEqualTo(resultCodeOf(ImageErrorCode.NO_IMAGE));
    }

    // given: 빈 파일과 정상 파일이 섞인 다중 업로드 요청
    // when: 이미지를 업로드한다.
    // then: 빈 파일은 실패 응답, 정상 파일은 저장 응답으로 각각 반환한다.
    @Test
    @DisplayName("다중 업로드에서 빈 파일은 실패하고 나머지 파일은 저장한다")
    void uploadImages_returnsPartialSuccessForEmptyFile() {
        Fixture fixture = new Fixture();
        fixture.stubSuccessfulStorageAndCreate();
        MultipartFile emptyFile = new MockMultipartFile("images", "empty.jpg", "image/jpeg", new byte[0]);

        List<ImageUploadResponse> responses = fixture.useCase.uploadImages(
            1L, 2L, new MultipartFile[] {emptyFile, imageFile()}
        );

        assertThat(responses).extracting(ImageUploadResponse::uploadStatus)
            .containsExactly(UploadStatus.FAILED, UploadStatus.STORED);
        assertThat(responses.getFirst().message()).isEqualTo("EMPTY_FILE");
    }

    // given: 파일 저장 중 ImageProcessException이 발생하는 이미지 파일
    // when: 이미지를 업로드한다.
    // then: DB 저장 없이 FILE SAVE FAILED 응답을 반환한다.
    @Test
    @DisplayName("파일 저장에 실패하면 파일 저장 실패 응답을 반환한다")
    void uploadImages_returnsFailureResponseWhenStorageFails() {
        Fixture fixture = new Fixture();
        when(fixture.imageFileStorage.saveImageWithThumbnail(any(byte[].class), any()))
            .thenThrow(new ImageProcessException(ImageErrorCode.SAVE_ERROR));

        ImageUploadResponse response = fixture.useCase
            .uploadImages(1L, 2L, new MultipartFile[] {imageFile()})
            .getFirst();

        assertThat(response.uploadStatus()).isEqualTo(UploadStatus.FAILED);
        assertThat(response.message()).isEqualTo("FILE SAVE FAILED");
    }

    // given: 파일 저장은 성공하고 ImageService 저장이 실패하는 이미지 파일
    // when: 이미지를 업로드한다.
    // then: 저장한 원본·섬네일 파일을 정리하고 SERVER SAVE FAILED 응답을 반환한다.
    @Test
    @DisplayName("DB 저장에 실패하면 저장한 파일을 정리하고 실패 응답을 반환한다")
    void uploadImages_cleansUpFilesWhenImageServiceCreateFails() {
        Fixture fixture = new Fixture();
        SavedFileInfo savedFileInfo = fixture.stubSuccessfulStorage();
        when(fixture.imageService.create(any())).thenThrow(new IllegalArgumentException("DB 저장 실패"));

        ImageUploadResponse response = fixture.useCase
            .uploadImages(1L, 2L, new MultipartFile[] {imageFile()})
            .getFirst();

        assertThat(response.uploadStatus()).isEqualTo(UploadStatus.FAILED);
        assertThat(response.message()).isEqualTo("SERVER SAVE FAILED");
        verify(fixture.imageFileStorage).cleanUp(savedFileInfo);
    }

    private static class Fixture {
        // mock 객체 모음
        private final ImageService imageService = mock(ImageService.class);
        private final ImageMetadataExtractor metadataExtractor = mock(ImageMetadataExtractor.class);
        private final ImageFileStorage imageFileStorage = mock(ImageFileStorage.class);
        private final TripService tripService = mock(TripService.class);
        private final MemberService memberService = mock(MemberService.class);
        private final PostService postService = mock(PostService.class);
        private final Member owner = mock(Member.class);
        private final Trip trip = mock(Trip.class);
        private final Post post = mock(Post.class);
        private final ImageUploadUseCase useCase = new ImageUploadUseCase(
            imageService, metadataExtractor, imageFileStorage, tripService, memberService, postService
        );

        private Fixture() {
            when(owner.getId()).thenReturn(1L);
            when(memberService.findById(1L)).thenReturn(owner);
            when(tripService.findOwnedTrip(2L, 1L)).thenReturn(trip);
            when(postService.getPost(trip, 3L)).thenReturn(post);
            when(metadataExtractor.extract(any(byte[].class))).thenReturn(new ImageInfo());
        }

        private SavedFileInfo stubSuccessfulStorage() {
            //파일 저장소 정상 저장 시나리오
            SavedFileInfo saved = new SavedFileInfo("/images/origin.jpg", "/images/thumb.jpg", 100L, "image/jpeg");
            when(imageFileStorage.saveImageWithThumbnail(any(byte[].class), any())).thenReturn(saved);
            return saved;
        }

        private void stubSuccessfulStorageAndCreate() {
            // 파일 저장소 정상 저장, Image Create 성공 시나리오
            stubSuccessfulStorage();
            when(imageService.create(any())).thenReturn(serviceResponse());
        }
    }

    private static MultipartFile imageFile() {
        return new MockMultipartFile("images", "image.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private static ImageServiceResponse serviceResponse() {
        return new ImageServiceResponse(
            10L, 1L, 2L, null, "/images/origin.jpg", "/images/thumb.jpg", "image/jpeg",
            null, null, null, null, UploadStatus.STORED
        );
    }

    private static String resultCodeOf(ImageErrorCode errorCode) {
        return "%s-%s".formatted(errorCode.getCode(), errorCode.getDomain().getCode());
    }
}
