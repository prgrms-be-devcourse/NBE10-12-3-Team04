package com.triptrace.domain.trip.tripAuto.service;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.geocoding.ReverseGeocodingResult;
import com.triptrace.domain.marker.marker.geocoding.ReverseGeocodingClient;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripAuto.dto.TripAutoRecordResponse;
import com.triptrace.domain.trip.tripAuto.error.TripAutoErrorCode;
import com.triptrace.global.app.Domain;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class TripAutoRecordServiceTest {

    @Autowired
    private TripAutoRecordService tripAutoRecordService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MarkerRepository markerRepository;

    @MockitoBean
    private ReverseGeocodingClient reverseGeocodingClient;

    @Test
    @DisplayName("메타데이터가 완전한 이미지만 자동 기록으로 생성한다")
    void createAutoRecordsWithUsableImages() {
        Member owner = createMember("autoRecordOwner");
        Trip trip = createTrip(owner, "부산 여행");
        Image usableImage = createImage(
            owner,
            trip,
            "usable.jpg",
            new BigDecimal("35.179554312"),
            new BigDecimal("129.075641612"),
            LocalDateTime.of(2026, 6, 27, 10, 0)
        );
        Image skippedImage = createImage(owner, trip, "skipped.jpg", null, null, null);
        when(reverseGeocodingClient.findLocation(
            usableImage.getGpsLat(),
            usableImage.getGpsLng()
        )).thenReturn(new ReverseGeocodingResult("대한민국", "부산광역시", "광안리"));

        TripAutoRecordResponse response = tripAutoRecordService.createAutoRecords(
            trip.getId(),
            owner.getId()
        );

        assertThat(response.generatedPostCount()).isEqualTo(1);
        assertThat(response.generatedMarkerCount()).isEqualTo(1);
        assertThat(response.usedImageCount()).isEqualTo(1);
        assertThat(response.skippedImageCount()).isEqualTo(1);
        assertThat(response.records().getFirst().title()).isEqualTo("광안리 근처");
        Post post = postRepository.findById(response.records().getFirst().postId()).orElseThrow();
        Marker marker = markerRepository.findByPostId(post.getId()).orElseThrow();
        assertThat(marker.getCenterLat()).isEqualByComparingTo("35.1795543");
        assertThat(imageRepository.findById(usableImage.getId()).orElseThrow().getPost()).isEqualTo(post);
        assertThat(imageRepository.findById(skippedImage.getId()).orElseThrow().getPost()).isNull();
        Trip foundTrip = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(foundTrip.getRepresentativeImage()).isEqualTo(usableImage);
        assertThat(foundTrip.getCountry()).isEqualTo("대한민국");
        assertThat(foundTrip.getCity()).isEqualTo("부산광역시");
    }

    @Test
    @DisplayName("역지오코딩에 실패해도 자동 기록을 생성하고 기존 대표이미지를 유지한다")
    void createAutoRecordsWithoutGeocodingResult() {
        Member owner = createMember("geocodingFallbackOwner");
        Trip trip = createTrip(owner, "위치 미정 여행");
        Image existingRepresentative = createImage(owner, trip, "cover.jpg", null, null, null);
        trip.changeRepresentativeImage(existingRepresentative);
        Image usableImage = createImage(
            owner,
            trip,
            "record.jpg",
            new BigDecimal("37.566535012"),
            new BigDecimal("126.977969212"),
            LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        when(reverseGeocodingClient.findLocation(
            usableImage.getGpsLat(),
            usableImage.getGpsLng()
        )).thenReturn(null);

        TripAutoRecordResponse response = tripAutoRecordService.createAutoRecords(
            trip.getId(),
            owner.getId()
        );

        assertThat(response.records().getFirst().title()).isEqualTo("위치 미정 근처");
        assertThat(response.records().getFirst().location()).isNull();
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getRepresentativeImage())
            .isEqualTo(existingRepresentative);
    }

    @Test
    @DisplayName("여행기 소유자가 아니면 자동 기록을 생성할 수 없다")
    void createAutoRecordsByNotOwner() {
        Member owner = createMember("autoOwner");
        Member other = createMember("autoOther");
        Trip trip = createTrip(owner, "자동 기록 여행");

        assertThatThrownBy(() -> tripAutoRecordService.createAutoRecords(trip.getId(), other.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("%s-%s : %s".formatted(
                TripAutoErrorCode.FORBIDDEN.getCode(),
                Domain.TRIP.getCode(),
                TripAutoErrorCode.FORBIDDEN.getMessage()
            ));
    }

    @Test
    @DisplayName("존재하지 않는 여행기에는 자동 기록을 생성할 수 없다")
    void createAutoRecordsForUnknownTrip() {
        assertThatThrownBy(() -> tripAutoRecordService.createAutoRecords(Long.MAX_VALUE, Long.MAX_VALUE))
            .isInstanceOf(ServiceException.class)
            .hasMessage("%s-%s : %s".formatted(
                TripAutoErrorCode.TRIP_NOT_FOUND.getCode(),
                Domain.TRIP.getCode(),
                TripAutoErrorCode.TRIP_NOT_FOUND.getMessage()
            ));
    }

    @Test
    @DisplayName("클러스터 첫 사진과 2시간을 초과해 차이나는 이미지는 새 클러스터로 분리한다")
    void clusterImagesByClusterStartTime() {
        TripAutoRecordService service = new TripAutoRecordService(
            null,
            null,
            null,
            null,
            null
        );
        Image first = imageCapturedAt(LocalDateTime.of(2026, 6, 30, 14, 0));
        Image second = imageCapturedAt(LocalDateTime.of(2026, 6, 30, 15, 50));
        Image third = imageCapturedAt(LocalDateTime.of(2026, 6, 30, 17, 40));

        List<List<Image>> clusters = ReflectionTestUtils.invokeMethod(
            service,
            "clusterImages",
            List.of(first, second, third)
        );

        assertThat(clusters).hasSize(2);
        assertThat(clusters.get(0)).containsExactly(first, second);
        assertThat(clusters.get(1)).containsExactly(third);
    }

    @Test
    @DisplayName("자동 생성 후 Trip 국가/도시는 첫 마커 기준, 기간은 첫 사진과 마지막 사진 기준으로 보정한다")
    void applyTripAutoRecordDefaults() {
        TripAutoRecordService service = new TripAutoRecordService(
            null,
            null,
            null,
            null,
            null
        );
        Trip trip = new Trip(
            null,
            "부산 여행",
            "기존 국가",
            "기존 도시",
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 2, 0, 0),
            true
        );
        Image first = imageCapturedAt(LocalDateTime.of(2026, 6, 27, 19, 37, 55));
        Image last = imageCapturedAt(LocalDateTime.of(2026, 6, 30, 10, 15));

        ReflectionTestUtils.invokeMethod(
            service,
            "applyTripAutoRecordDefaults",
            trip,
            List.of(first, last),
            new ReverseGeocodingResult("대한민국", "부산광역시", "부산광역시 남구 문현동")
        );

        assertThat(trip.getCountry()).isEqualTo("대한민국");
        assertThat(trip.getCity()).isEqualTo("부산광역시");
        assertThat(trip.getStartDate()).isEqualTo(LocalDateTime.of(2026, 6, 27, 19, 37, 55));
        assertThat(trip.getEndDate()).isEqualTo(LocalDateTime.of(2026, 6, 30, 10, 15));
    }

    @Test
    @DisplayName("자동 생성 마커 중심 좌표는 소수점 7자리까지 버림 처리한다")
    void truncateCoordinateToSevenDecimalPlaces() {
        TripAutoRecordService service = new TripAutoRecordService(
            null,
            null,
            null,
            null,
            null
        );

        BigDecimal coordinate = ReflectionTestUtils.invokeMethod(
            service,
            "truncateCoordinate",
            new BigDecimal("37.123456789")
        );

        assertThat(coordinate).isEqualByComparingTo(new BigDecimal("37.1234567"));
    }

    private Image imageCapturedAt(LocalDateTime capturedAt) {
        Image image = new Image(
            null,
            null,
            null,
            "https://example.com/image.jpg",
            "https://example.com/image-thumbnail.jpg",
            1000L,
            "image/jpeg",
            UploadStatus.STORED
        );
        ReflectionTestUtils.setField(image, "capturedAt", capturedAt);

        return image;
    }

    private Member createMember(String username) {
        return memberRepository.save(new Member(
            "%s@test.com".formatted(username),
            username,
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));
    }

    private Trip createTrip(Member owner, String title) {
        return tripRepository.save(new Trip(
            owner,
            title,
            "기존 국가",
            "기존 도시",
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 2, 0, 0),
            true
        ));
    }

    private Image createImage(
        Member owner,
        Trip trip,
        String fileName,
        BigDecimal gpsLat,
        BigDecimal gpsLng,
        LocalDateTime capturedAt
    ) {
        return imageRepository.save(new Image(
            owner,
            trip,
            null,
            "/images/serving/%s".formatted(fileName),
            "/images/thumbnail/%s".formatted(fileName),
            1024L,
            "image/jpeg",
            gpsLat,
            gpsLng,
            capturedAt,
            "camera",
            UploadStatus.STORED
        ));
    }
}
