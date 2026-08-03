package com.triptrace.domain.marker.marker.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.triptrace.domain.marker.marker.dto.MarkerResponse;
import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse;
import com.triptrace.domain.marker.marker.entity.MarkerSource;
import com.triptrace.domain.marker.marker.service.MarkerService;
import com.triptrace.global.app.Domain;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiV1MarkerControllerTest {

    private static final long POST_ID = 10L;
    private static final long MARKER_ID = 20L;
    private static final long MEMBER_ID = 30L;
    private static final BigDecimal LATITUDE = new BigDecimal("37.5665350");
    private static final BigDecimal LONGITUDE = new BigDecimal("126.9779692");
    private static final String SUCCESS_CODE = "200-" + Domain.MARKER.getCode();
    private static final String CREATED_CODE = "201-" + Domain.MARKER.getCode();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MarkerService markerService;

    @Test
    @DisplayName("마커 생성 API는 인증 사용자와 요청을 서비스에 전달한다")
    void createMarker() throws Exception {
        MarkerResponse response = markerResponse("서울시청", LocalDateTime.of(2026, 8, 3, 10, 30));
        when(markerService.createMarker(
            org.mockito.ArgumentMatchers.eq(POST_ID),
            org.mockito.ArgumentMatchers.eq(MEMBER_ID),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(response);

        mvc.perform(post("/api/v1/posts/{postId}/markers", POST_ID)
                .with(csrf())
                .with(authentication(memberAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "centerLat": 37.5665350,
                      "centerLng": 126.9779692,
                      "placeName": "서울시청",
                      "visitedAt": "2026-08-03T10:30:00",
                      "source": "MANUAL"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value(CREATED_CODE))
            .andExpect(jsonPath("$.data.id").value(MARKER_ID))
            .andExpect(jsonPath("$.data.postId").value(POST_ID))
            .andExpect(jsonPath("$.data.placeName").value("서울시청"));

        verify(markerService).createMarker(
            org.mockito.ArgumentMatchers.eq(POST_ID),
            org.mockito.ArgumentMatchers.eq(MEMBER_ID),
            argThat(request ->
                request.centerLat().compareTo(LATITUDE) == 0
                    && request.centerLng().compareTo(LONGITUDE) == 0
                    && request.placeName().equals("서울시청")
                    && request.source() == MarkerSource.MANUAL
            )
        );
    }

    @Test
    @DisplayName("마커 생성 요청에 source가 없으면 400을 반환한다")
    void createMarkerWithoutSource() throws Exception {
        mvc.perform(post("/api/v1/posts/{postId}/markers", POST_ID)
                .with(csrf())
                .with(authentication(memberAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "centerLat": 37.5665350,
                      "centerLng": 126.9779692,
                      "placeName": "서울시청"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verifyNoInteractions(markerService);
    }

    @Test
    @DisplayName("게시글의 마커 목록 조회 API")
    void getMarkers() throws Exception {
        when(markerService.getMarkers(POST_ID))
            .thenReturn(List.of(markerResponse("서울시청", LocalDateTime.of(2026, 8, 3, 10, 30))));

        mvc.perform(get("/api/v1/posts/{postId}/markers", POST_ID)
                .with(authentication(memberAuthentication())))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value(SUCCESS_CODE))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(MARKER_ID))
            .andExpect(jsonPath("$.data[0].placeName").value("서울시청"));

        verify(markerService).getMarkers(POST_ID);
    }

    @Test
    @DisplayName("마커 상세 조회 API")
    void getMarker() throws Exception {
        when(markerService.getMarker(MARKER_ID))
            .thenReturn(markerResponse("서울시청", LocalDateTime.of(2026, 8, 3, 10, 30)));

        mvc.perform(get("/api/v1/posts/markers/{markerId}", MARKER_ID)
                .with(authentication(memberAuthentication())))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value(SUCCESS_CODE))
            .andExpect(jsonPath("$.data.id").value(MARKER_ID))
            .andExpect(jsonPath("$.data.centerLat").value(37.5665350));

        verify(markerService).getMarker(MARKER_ID);
    }

    @Test
    @DisplayName("마커 장소 후보 조회 API는 인증 사용자를 서비스에 전달한다")
    void getPlaceCandidates() throws Exception {
        when(markerService.getPlaceCandidates(MARKER_ID, MEMBER_ID)).thenReturn(List.of(placeCandidate()));

        mvc.perform(get("/api/v1/posts/markers/{markerId}/place-candidates", MARKER_ID)
                .with(authentication(memberAuthentication())))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value(SUCCESS_CODE))
            .andExpect(jsonPath("$.data[0].placeId").value("place-1"))
            .andExpect(jsonPath("$.data[0].name").value("서울시청"));

        verify(markerService).getPlaceCandidates(MARKER_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("장소 검색 API는 검색어를 서비스에 전달한다")
    void searchPlaces() throws Exception {
        when(markerService.searchPlaces("서울시청")).thenReturn(List.of(placeCandidate()));

        mvc.perform(get("/api/v1/places/search")
                .with(authentication(memberAuthentication()))
                .param("keyword", "서울시청"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value(SUCCESS_CODE))
            .andExpect(jsonPath("$.data[0].name").value("서울시청"));

        verify(markerService).searchPlaces("서울시청");
    }

    @Test
    @DisplayName("주변 장소 조회 API는 위도와 경도를 서비스에 전달한다")
    void findNearbyPlaces() throws Exception {
        when(markerService.findNearbyPlaces(LATITUDE, LONGITUDE)).thenReturn(List.of(placeCandidate()));

        mvc.perform(get("/api/v1/places/nearby")
                .with(authentication(memberAuthentication()))
                .param("latitude", LATITUDE.toPlainString())
                .param("longitude", LONGITUDE.toPlainString()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value(SUCCESS_CODE))
            .andExpect(jsonPath("$.data[0].latitude").value(37.5665350))
            .andExpect(jsonPath("$.data[0].longitude").value(126.9779692));

        verify(markerService).findNearbyPlaces(LATITUDE, LONGITUDE);
    }

    @Test
    @DisplayName("마커 수정 API는 인증 사용자와 요청을 서비스에 전달한다")
    void modifyMarker() throws Exception {
        LocalDateTime modifiedVisitedAt = LocalDateTime.of(2026, 8, 3, 15, 45);
        when(markerService.modifyMarker(
            org.mockito.ArgumentMatchers.eq(MARKER_ID),
            org.mockito.ArgumentMatchers.eq(MEMBER_ID),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(markerResponse("광화문", modifiedVisitedAt));

        mvc.perform(patch("/api/v1/posts/markers/{markerId}", MARKER_ID)
                .with(csrf())
                .with(authentication(memberAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "centerLat": 37.5665350,
                      "centerLng": 126.9779692,
                      "placeName": "광화문",
                      "visitedAt": "2026-08-03T15:45:00",
                      "source": "MANUAL"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value(SUCCESS_CODE))
            .andExpect(jsonPath("$.data.placeName").value("광화문"))
            .andExpect(jsonPath("$.data.visitedAt").value("2026-08-03T15:45:00"));

        verify(markerService).modifyMarker(
            org.mockito.ArgumentMatchers.eq(MARKER_ID),
            org.mockito.ArgumentMatchers.eq(MEMBER_ID),
            argThat(request ->
                request.placeName().equals("광화문")
                    && request.visitedAt().equals(modifiedVisitedAt)
                    && request.source() == MarkerSource.MANUAL
            )
        );
    }

    @Test
    @DisplayName("마커 삭제 API는 인증 사용자와 마커 ID를 서비스에 전달한다")
    void deleteMarker() throws Exception {
        mvc.perform(delete("/api/v1/posts/markers/{markerId}", MARKER_ID)
                .with(csrf())
            .with(authentication(memberAuthentication())))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value(SUCCESS_CODE))
            .andExpect(jsonPath("$.msg").value("마커가 삭제되었습니다."));

        verify(markerService).deleteMarker(MARKER_ID, MEMBER_ID);
    }

    private UsernamePasswordAuthenticationToken memberAuthentication() {
        return new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());
    }

    private MarkerResponse markerResponse(String placeName, LocalDateTime visitedAt) {
        return new MarkerResponse(
            MARKER_ID,
            POST_ID,
            LATITUDE,
            LONGITUDE,
            placeName,
            visitedAt,
            MarkerSource.MANUAL,
            null,
            null,
            LocalDateTime.of(2026, 8, 3, 9, 0),
            LocalDateTime.of(2026, 8, 3, 9, 0)
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
}
