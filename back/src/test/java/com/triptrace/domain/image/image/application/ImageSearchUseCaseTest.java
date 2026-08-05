package com.triptrace.domain.image.image.application;

import com.triptrace.domain.image.image.dto.response.ImageResponse;
import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.service.ImageService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageSearchUseCaseTest {

    // given: 소유자의 이미지 서비스 응답 목록
    // when: 소유자 ID로 이미지를 조회한다.
    // then: API 응답 형태의 이미지 목록으로 변환해 반환한다.
    @Test
    @DisplayName("소유자 이미지 목록을 API 응답으로 변환한다")
    void getImages_mapsServiceResponsesToImageResponses() {
        ImageService imageService = mock(ImageService.class);
        ImageSearchUseCase useCase = new ImageSearchUseCase(imageService);
        when(imageService.findWithOwner(1L)).thenReturn(List.of(serviceResponse(10L), serviceResponse(20L)));

        List<ImageResponse> responses = useCase.getImages(1L);

        assertThat(responses)
            .extracting(ImageResponse::getId)
            .containsExactly(10L, 20L);
        assertThat(responses.getFirst().getOriginalUrl()).isEqualTo("/images/10.jpg");
        verify(imageService).findWithOwner(1L);
    }

    // given: 이미지가 없는 소유자
    // when: 소유자 ID로 이미지를 조회한다.
    // then: 빈 목록을 반환한다.
    @Test
    @DisplayName("이미지가 없는 소유자는 빈 목록을 반환한다")
    void getImages_returnsEmptyListWhenOwnerHasNoImages() {
        ImageService imageService = mock(ImageService.class);
        ImageSearchUseCase useCase = new ImageSearchUseCase(imageService);
        when(imageService.findWithOwner(1L)).thenReturn(List.of());

        assertThat(useCase.getImages(1L)).isEmpty();
    }

    private ImageServiceResponse serviceResponse(Long id) {
        return new ImageServiceResponse(
            id, 1L, 2L, 3L, "/images/%d.jpg".formatted(id), "/images/%d-thumb.jpg".formatted(id),
            "image/jpeg", null, null, null, null, UploadStatus.STORED
        );
    }
}
