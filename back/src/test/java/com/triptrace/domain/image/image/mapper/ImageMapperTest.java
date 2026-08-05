package com.triptrace.domain.image.image.mapper;

import com.triptrace.domain.image.image.dto.response.ImageResponse;
import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.response.storage.StoredImageFile;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.processing.ImageInfo;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.trip.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ImageMapperTest {

    private Member owner;
    private Trip trip;
    private Post post;
    private ImageInfo imageInfo;
    private StoredImageFile storedImageFile;

    @BeforeEach
    void setUp() {
        // owner, trip, post: 이미지 연관관계 검증에 사용하는 기본 데이터
        owner = new Member("user@example.com", "traveler", "passwordHash", null, MemberStatus.ACTIVE);
        trip = new Trip(
            owner, "교토 여행", "일본", "교토",
            LocalDateTime.of(2024, 4, 1, 0, 0), LocalDateTime.of(2024, 4, 5, 0, 0), true
        );
        post = new Post(trip, LocalDate.of(2024, 4, 1), "첫날", "교토에 도착했다.");

        // imageInfo: EXIF에서 추출되는 메타데이터
        imageInfo = new ImageInfo();
        ReflectionTestUtils.setField(imageInfo, "latitude", 35.011636D);
        ReflectionTestUtils.setField(imageInfo, "longitude", 135.768029D);
        ReflectionTestUtils.setField(imageInfo, "capturedAt", LocalDateTime.of(2024, 4, 1, 10, 30));
        ReflectionTestUtils.setField(imageInfo, "maker", "Apple");
        ReflectionTestUtils.setField(imageInfo, "model", "iPhone 15");

        // storedImageFile: 파일 저장소가 반환하는 저장 결과
        storedImageFile = new StoredImageFile(
            "https://example.com/images/kyoto.jpg",
            "https://example.com/images/kyoto-thumb.jpg",
            1024L,
            "image/jpeg"
        );
    }

    // toEntity(Member owner, Trip trip, ImageInfo imageInfo, StoredImageFile storedImageFile)
    // given: 정상적인 연관관계와 파일·EXIF 정보를 제공한다.
    // when: toEntity를 호출한다.
    // then: post 없이 저장 가능한 Image를 반환한다.
    @Test
    @DisplayName("게시글 없이 업로드한 이미지 엔티티를 생성한다")
    void toEntity_withoutPost_returnsStoredImage() {
        Image image = ImageMapper.toEntity(owner, trip, imageInfo, storedImageFile);

        assertThat(image.getOwner()).isSameAs(owner);
        assertThat(image.getTrip()).isSameAs(trip);
        assertThat(image.getPost()).isNull();
        assertThat(image.getOriginalFileUrl()).isEqualTo(storedImageFile.getImageFileUrl());
        assertThat(image.getThumbnailUrl()).isEqualTo(storedImageFile.getThumbnailImageFileUrl());
        assertThat(image.getFileSize()).isEqualTo(storedImageFile.getFileSize());
        assertThat(image.getMimeType()).isEqualTo(storedImageFile.getMimeType());
        assertThat(image.getGpsLat()).isEqualByComparingTo(BigDecimal.valueOf(35.011636D));
        assertThat(image.getGpsLng()).isEqualByComparingTo(BigDecimal.valueOf(135.768029D));
        assertThat(image.getCapturedAt()).isEqualTo(LocalDateTime.of(2024, 4, 1, 10, 30));
        assertThat(image.getDeviceInfo()).isEqualTo("Apple - iPhone 15");
        assertThat(image.getUploadStatus()).isEqualTo(UploadStatus.STORED);
    }

    // toEntity(Member owner, Trip trip, Post post, ImageInfo imageInfo, StoredImageFile storedImageFile)
    // imageFileUrl이 공백이면 파일 저장 실패로 판단한다.
    @Test
    @DisplayName("이미지 파일 URL이 공백이면 FAILED 상태의 이미지 엔티티를 생성한다")
    void toEntity_withBlankImageFileUrl_returnsFailedImage() {
        StoredImageFile blankUrlFile = new StoredImageFile(" ", null, 1024L, "image/jpeg");

        Image image = ImageMapper.toEntity(owner, trip, post, imageInfo, blankUrlFile);

        assertThat(image.getOwner()).isSameAs(owner);
        assertThat(image.getTrip()).isSameAs(trip);
        assertThat(image.getPost()).isSameAs(post);
        assertThat(image.getUploadStatus()).isEqualTo(UploadStatus.FAILED);
    }

    // 위도와 경도는 모두 존재할 때만 Image의 GPS 정보로 설정한다.
    @Test
    @DisplayName("위도 또는 경도 하나만 있으면 GPS 정보를 설정하지 않는다")
    void toEntity_withIncompleteGps_doesNotSetGps() {
        ReflectionTestUtils.setField(imageInfo, "longitude", null);

        Image image = ImageMapper.toEntity(owner, trip, post, imageInfo, storedImageFile);

        assertThat(image.getGpsLat()).isNull();
        assertThat(image.getGpsLng()).isNull();
    }

    // toImageResponse(ImageServiceResponse imageServiceResponse)
    // 정상적인 ImageServiceResponse를 받으면 API 응답용 ImageResponse를 반환한다.
    @Test
    @DisplayName("서비스 응답을 이미지 API 응답으로 변환한다")
    void toImageResponse_returnsImageResponse() {
        ImageServiceResponse serviceResponse = new ImageServiceResponse(
            1L, 2L, 3L, 4L,
            "https://example.com/images/kyoto.jpg",
            "https://example.com/images/kyoto-thumb.jpg",
            "image/jpeg",
            BigDecimal.valueOf(35.011636D), BigDecimal.valueOf(135.768029D),
            LocalDateTime.of(2024, 4, 1, 10, 30), "Apple - iPhone 15", UploadStatus.STORED
        );

        ImageResponse response = ImageMapper.toImageResponse(serviceResponse);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOwnerId()).isEqualTo(2L);
        assertThat(response.getTripId()).isEqualTo(3L);
        assertThat(response.getPostId()).isEqualTo(4L);
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com/images/kyoto.jpg");
        assertThat(response.getThumbnailUrl()).isEqualTo("https://example.com/images/kyoto-thumb.jpg");
    }
}
