package com.triptrace.domain.marker.marker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.triptrace.domain.marker.marker.dto.MarkerCreateRequest;
import com.triptrace.domain.marker.marker.dto.MarkerModifyRequest;
import com.triptrace.domain.marker.marker.dto.MarkerResponse;
import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.entity.MarkerSource;
import com.triptrace.domain.marker.marker.error.MarkerErrorCode;
import com.triptrace.domain.marker.marker.place.GooglePlacesClient;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.global.app.Domain;
import com.triptrace.global.exception.ServiceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MarkerServiceTest {

    private static final long POST_ID = 10L;
    private static final long MARKER_ID = 20L;
    private static final long OWNER_ID = 30L;
    private static final long OTHER_MEMBER_ID = 40L;
    private static final LocalDate POST_DATE = LocalDate.of(2026, 8, 3);
    private static final BigDecimal LATITUDE = new BigDecimal("37.5665350");
    private static final BigDecimal LONGITUDE = new BigDecimal("126.9779692");

    private MarkerRepository markerRepository;
    private PostRepository postRepository;
    private GooglePlacesClient googlePlacesClient;
    private MarkerService markerService;

    @BeforeEach
    void setUp() {
        markerRepository = mock(MarkerRepository.class);
        postRepository = mock(PostRepository.class);
        googlePlacesClient = mock(GooglePlacesClient.class);
        markerService = new MarkerService(markerRepository, postRepository, googlePlacesClient);
    }

    @Test
    @DisplayName("소유자는 게시글에 마커를 생성할 수 있다")
    void createMarkerByOwner() {
        Post post = postOwnedBy(OWNER_ID);
        MarkerCreateRequest request = new MarkerCreateRequest(
            LATITUDE,
            LONGITUDE,
            "서울시청",
            LocalDateTime.of(2026, 8, 3, 10, 30),
            MarkerSource.MANUAL
        );
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(markerRepository.save(any(Marker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MarkerResponse response = markerService.createMarker(POST_ID, OWNER_ID, request);

        ArgumentCaptor<Marker> markerCaptor = ArgumentCaptor.forClass(Marker.class);
        verify(markerRepository).save(markerCaptor.capture());
        Marker saved = markerCaptor.getValue();
        assertThat(saved.getPost()).isSameAs(post);
        assertThat(saved.getCenterLat()).isEqualByComparingTo(LATITUDE);
        assertThat(saved.getCenterLng()).isEqualByComparingTo(LONGITUDE);
        assertThat(saved.getPlaceName()).isEqualTo("서울시청");
        assertThat(saved.getVisitedAt()).isEqualTo(LocalDateTime.of(2026, 8, 3, 10, 30));
        assertThat(saved.getSource()).isEqualTo(MarkerSource.MANUAL);
        assertThat(response.postId()).isEqualTo(POST_ID);
        assertThat(response.placeName()).isEqualTo("서울시청");
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 마커를 생성하면 실패한다")
    void createMarkerWithMissingPost() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertServiceException(
            () -> markerService.createMarker(POST_ID, OWNER_ID, createRequest()),
            MarkerErrorCode.POST_NOT_FOUND
        );

        verify(markerRepository, never()).save(any(Marker.class));
    }

    @Test
    @DisplayName("게시글 소유자가 아니면 마커를 생성할 수 없다")
    void createMarkerByNotOwner() {
        Post post = postOwnedBy(OWNER_ID);
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertServiceException(
            () -> markerService.createMarker(POST_ID, OTHER_MEMBER_ID, createRequest()),
            MarkerErrorCode.FORBIDDEN
        );

        verify(markerRepository, never()).save(any(Marker.class));
    }

    @Test
    @DisplayName("게시글의 마커 목록을 조회한다")
    void getMarkers() {
        Post post = postOwnedBy(OWNER_ID);
        Marker marker = marker(post, "서울시청", LocalDateTime.of(2026, 8, 3, 10, 0));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(markerRepository.findByPostId(POST_ID)).thenReturn(Optional.of(marker));

        List<MarkerResponse> responses = markerService.getMarkers(POST_ID);

        assertThat(responses).extracting(MarkerResponse::placeName).containsExactly("서울시청");
        verify(markerRepository).findByPostId(POST_ID);
    }

    @Test
    @DisplayName("게시글에 마커가 없으면 빈 목록을 반환한다")
    void getMarkersReturnsEmptyList() {
        Post post = postOwnedBy(OWNER_ID);
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(markerRepository.findByPostId(POST_ID)).thenReturn(Optional.empty());

        List<MarkerResponse> responses = markerService.getMarkers(POST_ID);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 마커 목록을 조회하면 실패한다")
    void getMarkersWithMissingPost() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertServiceException(() -> markerService.getMarkers(POST_ID), MarkerErrorCode.POST_NOT_FOUND);

        verify(markerRepository, never()).findByPostId(any());
    }

    @Test
    @DisplayName("마커를 단건 조회한다")
    void getMarker() {
        Marker marker = marker(postOwnedBy(OWNER_ID), "서울시청", LocalDateTime.of(2026, 8, 3, 10, 0));
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.of(marker));

        MarkerResponse response = markerService.getMarker(MARKER_ID);

        assertThat(response.placeName()).isEqualTo("서울시청");
        assertThat(response.postId()).isEqualTo(POST_ID);
    }

    @Test
    @DisplayName("존재하지 않는 마커를 단건 조회하면 실패한다")
    void getMissingMarker() {
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.empty());

        assertServiceException(() -> markerService.getMarker(MARKER_ID), MarkerErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("소유자는 마커 좌표 주변의 장소 후보를 조회할 수 있다")
    void getPlaceCandidatesByOwner() {
        Marker marker = marker(postOwnedBy(OWNER_ID), "서울시청", LocalDateTime.of(2026, 8, 3, 10, 0));
        List<PlaceCandidateResponse> expected = List.of(placeCandidate());
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.of(marker));
        when(googlePlacesClient.findNearbyPlaces(LATITUDE, LONGITUDE)).thenReturn(expected);

        List<PlaceCandidateResponse> responses = markerService.getPlaceCandidates(MARKER_ID, OWNER_ID);

        assertThat(responses).isSameAs(expected);
        verify(googlePlacesClient).findNearbyPlaces(LATITUDE, LONGITUDE);
    }

    @Test
    @DisplayName("존재하지 않는 마커의 장소 후보를 조회하면 실패한다")
    void getPlaceCandidatesWithMissingMarker() {
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.empty());

        assertServiceException(
            () -> markerService.getPlaceCandidates(MARKER_ID, OWNER_ID),
            MarkerErrorCode.NOT_FOUND
        );

        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    @DisplayName("마커 소유자가 아니면 장소 후보를 조회할 수 없다")
    void getPlaceCandidatesByNotOwner() {
        Marker marker = marker(postOwnedBy(OWNER_ID), "서울시청", LocalDateTime.of(2026, 8, 3, 10, 0));
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.of(marker));

        assertServiceException(
            () -> markerService.getPlaceCandidates(MARKER_ID, OTHER_MEMBER_ID),
            MarkerErrorCode.FORBIDDEN
        );

        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    @DisplayName("검색어로 장소를 검색한다")
    void searchPlaces() {
        List<PlaceCandidateResponse> expected = List.of(placeCandidate());
        when(googlePlacesClient.searchPlaces("서울시청")).thenReturn(expected);

        List<PlaceCandidateResponse> responses = markerService.searchPlaces("서울시청");

        assertThat(responses).isSameAs(expected);
        verify(googlePlacesClient).searchPlaces("서울시청");
    }

    @Test
    @DisplayName("장소 검색어가 null이면 실패한다")
    void searchPlacesWithNullKeyword() {
        assertServiceException(() -> markerService.searchPlaces(null), MarkerErrorCode.KEYWORD_REQUIRED);

        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    @DisplayName("장소 검색어가 빈 문자열이면 실패한다")
    void searchPlacesWithEmptyKeyword() {
        assertServiceException(() -> markerService.searchPlaces(""), MarkerErrorCode.KEYWORD_REQUIRED);

        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    @DisplayName("장소 검색어가 공백뿐이면 실패한다")
    void searchPlacesWithBlankKeyword() {
        assertServiceException(() -> markerService.searchPlaces("   "), MarkerErrorCode.KEYWORD_REQUIRED);

        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    @DisplayName("위도와 경도로 주변 장소를 조회한다")
    void findNearbyPlaces() {
        List<PlaceCandidateResponse> expected = List.of(placeCandidate());
        when(googlePlacesClient.findNearbyPlaces(LATITUDE, LONGITUDE)).thenReturn(expected);

        List<PlaceCandidateResponse> responses = markerService.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertThat(responses).isSameAs(expected);
        verify(googlePlacesClient).findNearbyPlaces(LATITUDE, LONGITUDE);
    }

    @Test
    @DisplayName("위도가 없으면 주변 장소 조회에 실패한다")
    void findNearbyPlacesWithoutLatitude() {
        assertServiceException(
            () -> markerService.findNearbyPlaces(null, LONGITUDE),
            MarkerErrorCode.COORDINATES_REQUIRED
        );

        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    @DisplayName("경도가 없으면 주변 장소 조회에 실패한다")
    void findNearbyPlacesWithoutLongitude() {
        assertServiceException(
            () -> markerService.findNearbyPlaces(LATITUDE, null),
            MarkerErrorCode.COORDINATES_REQUIRED
        );

        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    @DisplayName("위도와 경도가 모두 없으면 주변 장소 조회에 실패한다")
    void findNearbyPlacesWithoutCoordinates() {
        assertServiceException(
            () -> markerService.findNearbyPlaces(null, null),
            MarkerErrorCode.COORDINATES_REQUIRED
        );

        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    @DisplayName("소유자는 마커를 수정하고 방문 날짜는 게시글 날짜에 맞춘다")
    void modifyMarkerByOwnerAlignsVisitedDate() {
        Post post = postOwnedBy(OWNER_ID);
        Marker marker = marker(post, "수정 전", LocalDateTime.of(2026, 8, 1, 8, 0));
        MarkerModifyRequest request = new MarkerModifyRequest(
            new BigDecimal("35.1795543"),
            new BigDecimal("129.0756416"),
            "부산시청",
            LocalDateTime.of(2030, 1, 1, 15, 45),
            MarkerSource.AUTO
        );
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.of(marker));

        MarkerResponse response = markerService.modifyMarker(MARKER_ID, OWNER_ID, request);

        assertThat(response.centerLat()).isEqualByComparingTo("35.1795543");
        assertThat(response.centerLng()).isEqualByComparingTo("129.0756416");
        assertThat(response.placeName()).isEqualTo("부산시청");
        assertThat(response.visitedAt()).isEqualTo(LocalDateTime.of(POST_DATE, request.visitedAt().toLocalTime()));
        assertThat(response.source()).isEqualTo(MarkerSource.AUTO);
    }

    @Test
    @DisplayName("방문 시각 없이 마커를 수정하면 방문 시각을 null로 유지한다")
    void modifyMarkerWithoutVisitedAt() {
        Marker marker = marker(postOwnedBy(OWNER_ID), "수정 전", LocalDateTime.of(2026, 8, 3, 8, 0));
        MarkerModifyRequest request = new MarkerModifyRequest(
            LATITUDE,
            LONGITUDE,
            "수정 후",
            null,
            MarkerSource.MANUAL
        );
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.of(marker));

        MarkerResponse response = markerService.modifyMarker(MARKER_ID, OWNER_ID, request);

        assertThat(response.visitedAt()).isNull();
        assertThat(response.placeName()).isEqualTo("수정 후");
    }

    @Test
    @DisplayName("존재하지 않는 마커를 수정하면 실패한다")
    void modifyMissingMarker() {
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.empty());

        assertServiceException(
            () -> markerService.modifyMarker(MARKER_ID, OWNER_ID, modifyRequest()),
            MarkerErrorCode.NOT_FOUND
        );
    }

    @Test
    @DisplayName("마커 소유자가 아니면 수정할 수 없다")
    void modifyMarkerByNotOwner() {
        Marker marker = marker(postOwnedBy(OWNER_ID), "수정 전", LocalDateTime.of(2026, 8, 3, 8, 0));
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.of(marker));

        assertServiceException(
            () -> markerService.modifyMarker(MARKER_ID, OTHER_MEMBER_ID, modifyRequest()),
            MarkerErrorCode.FORBIDDEN
        );

        assertThat(marker.getPlaceName()).isEqualTo("수정 전");
    }

    @Test
    @DisplayName("존재하지 않는 마커를 삭제하면 실패한다")
    void deleteMissingMarker() {
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.empty());

        assertServiceException(
            () -> markerService.deleteMarker(MARKER_ID, OWNER_ID),
            MarkerErrorCode.NOT_FOUND
        );
    }

    @Test
    @DisplayName("마커 소유자가 아니면 삭제할 수 없다")
    void deleteMarkerByNotOwner() {
        Marker marker = marker(postOwnedBy(OWNER_ID), "서울시청", null);
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.of(marker));

        assertServiceException(
            () -> markerService.deleteMarker(MARKER_ID, OTHER_MEMBER_ID),
            MarkerErrorCode.FORBIDDEN
        );
    }

    @Test
    @DisplayName("마커는 소유자도 직접 삭제할 수 없다")
    void deleteMarkerByOwnerIsNotAllowed() {
        Marker marker = marker(postOwnedBy(OWNER_ID), "서울시청", null);
        when(markerRepository.findById(MARKER_ID)).thenReturn(Optional.of(marker));

        assertServiceException(
            () -> markerService.deleteMarker(MARKER_ID, OWNER_ID),
            MarkerErrorCode.DELETE_NOT_ALLOWED
        );
    }

    private Post postOwnedBy(long ownerId) {
        Member owner = mock(Member.class);
        Trip trip = mock(Trip.class);
        Post post = mock(Post.class);
        when(owner.getId()).thenReturn(ownerId);
        when(trip.getOwner()).thenReturn(owner);
        when(post.getTrip()).thenReturn(trip);
        when(post.getId()).thenReturn(POST_ID);
        when(post.getDate()).thenReturn(POST_DATE);
        return post;
    }

    private Marker marker(Post post, String placeName, LocalDateTime visitedAt) {
        return new Marker(post, LATITUDE, LONGITUDE, placeName, visitedAt, MarkerSource.MANUAL);
    }

    private MarkerCreateRequest createRequest() {
        return new MarkerCreateRequest(
            LATITUDE,
            LONGITUDE,
            "서울시청",
            LocalDateTime.of(2026, 8, 3, 10, 30),
            MarkerSource.MANUAL
        );
    }

    private MarkerModifyRequest modifyRequest() {
        return new MarkerModifyRequest(
            LATITUDE,
            LONGITUDE,
            "수정 후",
            LocalDateTime.of(2026, 8, 3, 11, 0),
            MarkerSource.MANUAL
        );
    }

    private PlaceCandidateResponse placeCandidate() {
        return new PlaceCandidateResponse(
            "place-1",
            "서울시청",
            "서울특별시 중구 세종대로 110",
            LATITUDE,
            LONGITUDE,
            List.of("city_hall", "point_of_interest")
        );
    }

    private void assertServiceException(ThrowingCallable callable, MarkerErrorCode errorCode) {
        assertThatThrownBy(callable)
            .isInstanceOf(ServiceException.class)
            .hasMessage("%s-%s : %s".formatted(
                errorCode.getCode(),
                Domain.MARKER.getCode(),
                errorCode.getMessage()
            ));
    }
}
