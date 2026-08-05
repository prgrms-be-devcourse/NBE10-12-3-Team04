package com.triptrace.domain.trip.tripAuto.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.geocoding.ReverseGeocodingClient;
import com.triptrace.domain.marker.marker.geocoding.ReverseGeocodingResult;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripAuto.error.TripAutoErrorCode;
import com.triptrace.global.app.Domain;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TripAutoRecordControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ImageRepository imageRepository;

    @MockitoBean
    private ReverseGeocodingClient reverseGeocodingClient;

    @Test
    @WithMockUser
    @DisplayName("이미지 메타데이터로 여행 기록을 자동 생성한다")
    void createAutoRecords() throws Exception {
        Member owner = memberRepository.save(new Member(
            "auto-controller@test.com",
            "autoControllerOwner",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));
        Trip trip = tripRepository.save(new Trip(
            owner,
            "부산 여행",
            "한국",
            "부산",
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 2, 0, 0),
            true
        ));
        Image image = imageRepository.save(new Image(
            owner,
            trip,
            null,
            "/images/serving/busan.jpg",
            "/images/thumbnail/busan.jpg",
            1024L,
            "image/jpeg",
            new BigDecimal("35.1795543"),
            new BigDecimal("129.0756416"),
            LocalDateTime.of(2026, 7, 1, 10, 0),
            "camera",
            UploadStatus.STORED
        ));
        when(reverseGeocodingClient.findLocation(image.getGpsLat(), image.getGpsLng()))
            .thenReturn(new ReverseGeocodingResult("대한민국", "부산광역시", "광안리"));

        mvc.perform(post("/api/v1/trips/{tripId}/auto-records", trip.getId())
                .with(csrf())
                .with(authentication(auth(owner))))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-" + Domain.TRIP.getCode()))
            .andExpect(jsonPath("$.data.generatedPostCount").value(1))
            .andExpect(jsonPath("$.data.generatedMarkerCount").value(1))
            .andExpect(jsonPath("$.data.records[0].title").value("광안리 근처"));
    }

    @Test
    @WithMockUser
    @DisplayName("여행기 주인이 아니면 자동 생성을 요청할 수 없다")
    void createAutoRecordsForbidden() throws Exception {
        Member owner = createMember("auto-owner@test.com", "autoOwner");
        Member stranger = createMember("auto-stranger@test.com", "autoStranger");
        Trip trip = createTrip(owner);

        mvc.perform(post("/api/v1/trips/{tripId}/auto-records", trip.getId())
                .with(csrf())
                .with(authentication(auth(stranger))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.resultCode")
                .value("%s-%s".formatted(
                    TripAutoErrorCode.FORBIDDEN.getCode(),
                    Domain.TRIP.getCode()
                )));
    }

    @Test
    @WithMockUser
    @DisplayName("없는 여행기에 자동 생성을 요청하면 404다")
    void createAutoRecordsTripNotFound() throws Exception {
        Member member = createMember("auto-none@test.com", "autoNone");

        mvc.perform(post("/api/v1/trips/{tripId}/auto-records", -1L)
                .with(csrf())
                .with(authentication(auth(member))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.resultCode")
                .value("%s-%s".formatted(
                    TripAutoErrorCode.TRIP_NOT_FOUND.getCode(),
                    Domain.TRIP.getCode()
                )));
    }

    private Member createMember(String email, String username) {
        return memberRepository.save(new Member(email, username, "password1234", "imageUrl", MemberStatus.ACTIVE));
    }

    private Trip createTrip(Member owner) {
        return tripRepository.save(new Trip(
            owner,
            "부산 여행",
            "한국",
            "부산",
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 2, 0, 0),
            true
        ));
    }

    private UsernamePasswordAuthenticationToken auth(Member member) {
        return new UsernamePasswordAuthenticationToken(member.getId(), null, List.of());
    }
}
