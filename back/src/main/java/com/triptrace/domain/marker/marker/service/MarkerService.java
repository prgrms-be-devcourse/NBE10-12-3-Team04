package com.triptrace.domain.marker.marker.service;

import com.triptrace.domain.marker.marker.dto.MarkerCreateRequest;
import com.triptrace.domain.marker.marker.dto.MarkerModifyRequest;
import com.triptrace.domain.marker.marker.dto.MarkerResponse;
import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.error.MarkerErrorCode;
import com.triptrace.domain.marker.marker.place.GooglePlacesClient;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.global.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarkerService {
    private final MarkerRepository markerRepository;
    private final PostRepository postRepository;
    private final GooglePlacesClient googlePlacesClient;

    // 권한 체크
    private void validateOwner(Post post, Long memberId) {
        Long ownerId = post.getTrip().getOwner().getId();

        if (!ownerId.equals(memberId)) {
            throw new ServiceException(MarkerErrorCode.FORBIDDEN);
        }
    }

    // 생성
    @Transactional
    public MarkerResponse createMarker(Long postId, Long memberId, MarkerCreateRequest request) {

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException(MarkerErrorCode.POST_NOT_FOUND));

        validateOwner(post, memberId);

        Marker marker = new Marker(
            post,
            request.centerLat(),
            request.centerLng(),
            request.placeName(),
            request.visitedAt(),
            request.source()
        );

        Marker saved = markerRepository.save(marker);

        return new MarkerResponse(saved);
    }

    // 목록
    public List<MarkerResponse> getMarkers(Long postId) {

        postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException(MarkerErrorCode.POST_NOT_FOUND));

        return markerRepository.findByPostId(postId)
            .stream()
            .map(MarkerResponse::new)
            .toList();
    }

    // 상세
    public MarkerResponse getMarker(Long markerId) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException(MarkerErrorCode.NOT_FOUND));

        return new MarkerResponse(marker);
    }

    // 장소명 후보 조회
    public List<PlaceCandidateResponse> getPlaceCandidates(Long markerId, Long memberId) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException(MarkerErrorCode.NOT_FOUND));

        validateOwner(marker.getPost(), memberId);

        // 자동 생성 때는 지역명만 저장하고, 사용자가 수정 화면에서 펼칠 때만 주변 상호명을 조회한다.
        return googlePlacesClient.findNearbyPlaces(marker.getCenterLat(), marker.getCenterLng());
    }

    public List<PlaceCandidateResponse> searchPlaces(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new ServiceException(MarkerErrorCode.KEYWORD_REQUIRED);
        }

        return googlePlacesClient.searchPlaces(keyword);
    }

    public List<PlaceCandidateResponse> findNearbyPlaces(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new ServiceException(MarkerErrorCode.COORDINATES_REQUIRED);
        }

        return googlePlacesClient.findNearbyPlaces(latitude, longitude);
    }

    // 수정
    @Transactional
    public MarkerResponse modifyMarker(Long markerId, Long memberId, MarkerModifyRequest request) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException(MarkerErrorCode.NOT_FOUND));

        validateOwner(marker.getPost(), memberId);

        marker.modify(
            request.centerLat(),
            request.centerLng(),
            request.placeName(),
            alignVisitedAtWithPostDate(marker.getPost(), request.visitedAt()),
            request.source()
        );

        return new MarkerResponse(marker);
    }

    // 삭제
    public void deleteMarker(Long markerId, Long memberId) {
        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException(MarkerErrorCode.NOT_FOUND));

        validateOwner(marker.getPost(), memberId);
        throw new ServiceException(MarkerErrorCode.DELETE_NOT_ALLOWED);
    }

    private LocalDateTime alignVisitedAtWithPostDate(Post post, LocalDateTime visitedAt) {
        if (visitedAt == null) {
            return null;
        }
        return LocalDateTime.of(post.getDate(), visitedAt.toLocalTime());
    }

    @java.lang.SuppressWarnings("all")
    public MarkerService(final MarkerRepository markerRepository, final PostRepository postRepository, final GooglePlacesClient googlePlacesClient) {
        this.markerRepository = markerRepository;
        this.postRepository = postRepository;
        this.googlePlacesClient = googlePlacesClient;
    }
}
