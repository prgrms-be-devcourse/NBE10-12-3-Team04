package com.triptrace.domain.image.image.controller;

import com.triptrace.domain.image.image.application.ImageDeleteUseCase;
import com.triptrace.domain.image.image.application.ImageModifyUseCase;
import com.triptrace.domain.image.image.application.ImageSearchUseCase;
import com.triptrace.domain.image.image.application.ImageUploadUseCase;
import com.triptrace.domain.image.image.dto.response.ImageResponse;
import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.response.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.UploadStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = ApiV1ImageControllerTest.MockUseCaseConfig.class)
class ApiV1ImageControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ImageUploadUseCase imageUploadUseCase;
    @Autowired private ImageDeleteUseCase imageDeleteUseCase;
    @Autowired private ImageModifyUseCase imageModifyUseCase;
    @Autowired private ImageSearchUseCase imageSearchUseCase;

    // given: 인증된 사용자의 이미지 목록
    // when: GET /api/v1/images를 요청한다.
    // then: 소유자 ID를 use case에 전달하고 이미지 목록 HTTP 응답을 반환한다.
    @Test
    @WithMockUser
    @DisplayName("인증된 사용자의 이미지 목록을 반환한다")
    void list_returnsImagesForAuthenticatedOwner() throws Exception {
        when(imageSearchUseCase.getImages(1L)).thenReturn(List.of(new ImageResponse(
            10L, 1L, 2L, 3L, "/images/origin.jpg", "/images/thumb.jpg"
        )));

        mvc.perform(get("/api/v1/images").with(authentication(auth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data[0].id").value(10L))
            .andExpect(jsonPath("$.data[0].originalUrl").value("/images/origin.jpg"));

        verify(imageSearchUseCase).getImages(1L);
    }

    // given: 인증된 사용자의 정상 이미지 파일
    // when: POST /api/v1/trips/{tripId}/images를 요청한다.
    // then: 게시글 없이 업로드 use case를 호출하고 업로드 응답을 반환한다.
    @Test
    @WithMockUser
    @DisplayName("게시글 없이 여행 이미지를 업로드한다")
    void upload_withoutPost_delegatesMultipartFiles() throws Exception {
        when(imageUploadUseCase.uploadImages(anyLong(), anyLong(), any(MultipartFile[].class)))
            .thenReturn(List.of(uploadResponse()));

        mvc.perform(multipart("/api/v1/trips/{tripId}/images", 2L)
                .file(imageFile())
                .with(authentication(auth(1L)))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(10L))
            .andExpect(jsonPath("$.data[0].uploadStatus").value("STORED"));

        verify(imageUploadUseCase).uploadImages(anyLong(), anyLong(), any(MultipartFile[].class));
    }

    // given: 인증된 사용자의 정상 이미지 파일과 게시글 ID
    // when: POST /api/v1/trips/{tripId}/posts/{postId}/images를 요청한다.
    // then: 게시글 범위 업로드 use case를 호출한다.
    @Test
    @WithMockUser
    @DisplayName("게시글에 여행 이미지를 업로드한다")
    void upload_withPost_delegatesPostIdAndMultipartFiles() throws Exception {
        when(imageUploadUseCase.uploadImages(anyLong(), anyLong(), anyLong(), any(MultipartFile[].class)))
            .thenReturn(List.of(uploadResponse()));

        mvc.perform(multipart("/api/v1/trips/{tripId}/posts/{postId}/images", 2L, 3L)
                .file(imageFile())
                .with(authentication(auth(1L)))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(10L));

        verify(imageUploadUseCase).uploadImages(anyLong(), anyLong(), anyLong(), any(MultipartFile[].class));
    }

    // given: 게시글에 연결된 이미지 ID
    // when: DELETE /api/v1/trips/{tripId}/posts/{postId}/images/{imageId}를 요청한다.
    // then: 게시글 범위 이미지 삭제 use case를 호출하고 성공 응답을 반환한다.
    @Test
    @WithMockUser
    @DisplayName("게시글 범위에서 이미지 ID로 삭제한다")
    void deleteById_withPost_delegatesRequest() throws Exception {
        mvc.perform(delete("/api/v1/trips/{tripId}/posts/{postId}/images/{imageId}", 2L, 3L, 4L)
                .with(authentication(auth(1L)))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.msg").value("삭제 되었습니다."));

        verify(imageDeleteUseCase).deleteById(1L, 2L, 3L, 4L);
    }

    // given: 게시글 없이 삭제할 이미지 ID
    // when: DELETE /api/v1/trips/{tripId}/images/{imageId}를 요청한다.
    // then: 여행 범위 이미지 삭제 use case를 호출한다.
    @Test
    @WithMockUser
    @DisplayName("여행 범위에서 이미지 ID로 삭제한다")
    void deleteById_withoutPost_delegatesRequest() throws Exception {
        mvc.perform(delete("/api/v1/trips/{tripId}/images/{imageId}", 2L, 4L)
                .with(authentication(auth(1L)))
                .with(csrf()))
            .andExpect(status().isOk());

        verify(imageDeleteUseCase).deleteById(1L, 2L, 4L);
    }

    // given: 게시글에 연결된 이미지 URL
    // when: DELETE 요청에 imageUrl 파라미터를 전달한다.
    // then: URL 삭제 use case를 호출한다.
    @Test
    @WithMockUser
    @DisplayName("게시글 범위에서 이미지 URL로 삭제한다")
    void deleteByUrl_delegatesImageUrl() throws Exception {
        mvc.perform(delete("/api/v1/trips/{tripId}/posts/{postId}/images", 2L, 3L)
                .param("imageUrl", "/images/origin.jpg")
                .with(authentication(auth(1L)))
                .with(csrf()))
            .andExpect(status().isOk());

        verify(imageDeleteUseCase).deleteByUrl(1L, 2L, 3L, "/images/origin.jpg");
    }

    // given: 이미지와 새 게시글 ID
    // when: PATCH /api/v1/trips/{tripId}/images를 요청한다.
    // then: 게시글 변경 use case 결과를 응답 데이터로 반환한다.
    @Test
    @WithMockUser
    @DisplayName("이미지의 게시글 연결을 변경한다")
    void modify_delegatesIdsAndReturnsResponse() throws Exception {
        when(imageModifyUseCase.modifyById(1L, 2L, 3L, 4L)).thenReturn(serviceResponse());

        mvc.perform(patch("/api/v1/trips/{tripId}/images", 2L)
                .param("postId", "3")
                .param("imageId", "4")
                .with(authentication(auth(1L)))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(10L))
            .andExpect(jsonPath("$.data.postId").value(3L));

        verify(imageModifyUseCase).modifyById(1L, 2L, 3L, 4L);
    }

    // given: 게시글 연결을 해제할 이미지 ID
    // when: PATCH /api/v1/trips/{tripId}/images/{imageId}/unassign을 요청한다.
    // then: 연결 해제 use case 결과를 응답 데이터로 반환한다.
    @Test
    @WithMockUser
    @DisplayName("이미지의 게시글 연결을 해제한다")
    void unassign_delegatesIdsAndReturnsResponse() throws Exception {
        when(imageModifyUseCase.unassign(1L, 2L, 4L)).thenReturn(serviceResponse());

        mvc.perform(patch("/api/v1/trips/{tripId}/images/{imageId}/unassign", 2L, 4L)
                .with(authentication(auth(1L)))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(10L));

        verify(imageModifyUseCase).unassign(1L, 2L, 4L);
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile("images", "image.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});
    }

    private ImageUploadResponse uploadResponse() {
        return new ImageUploadResponse(
            "image.jpg", 10L, "/images/origin.jpg", "/images/thumb.jpg", "image/jpeg", UploadStatus.STORED, "SUCCESS"
        );
    }

    private ImageServiceResponse serviceResponse() {
        return new ImageServiceResponse(
            10L, 1L, 2L, 3L, "/images/origin.jpg", "/images/thumb.jpg", "image/jpeg",
            null, null, null, null, UploadStatus.STORED
        );
    }

    private UsernamePasswordAuthenticationToken auth(Long ownerId) {
        return new UsernamePasswordAuthenticationToken(ownerId, null, List.of());
    }

    @TestConfiguration
    static class MockUseCaseConfig {
        // primary는 실제 ImageUploadUseCase도 존재할 때, controller에는 이 mock을 우선 주입
        @Bean @Primary ImageUploadUseCase mockImageUploadUseCase() { return mock(ImageUploadUseCase.class); }
        @Bean @Primary ImageDeleteUseCase mockImageDeleteUseCase() { return mock(ImageDeleteUseCase.class); }
        @Bean @Primary ImageModifyUseCase mockImageModifyUseCase() { return mock(ImageModifyUseCase.class); }
        @Bean @Primary ImageSearchUseCase mockImageSearchUseCase() { return mock(ImageSearchUseCase.class); }
    }
}
