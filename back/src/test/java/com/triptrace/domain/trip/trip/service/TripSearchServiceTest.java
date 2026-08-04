package com.triptrace.domain.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.dto.TripSearchResponse;
import com.triptrace.domain.trip.trip.dto.TripSearchLocationResponse;
import com.triptrace.domain.trip.trip.dto.TripSearchScope;
import com.triptrace.domain.trip.trip.dto.TripSearchSort;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.global.exception.ServiceException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class TripSearchServiceTest {

    @Autowired
    private TripSearchService tripSearchService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PostRepository postRepository;

    private Member owner;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        owner = memberRepository.save(
            new Member(
                unique + "@test.com",
                "search-" + unique,
                "password",
                null,
                MemberStatus.ACTIVE
            )
        );
    }

    @Test
    @DisplayName("검색 조건이 없으면 공개 여행기만 최신순으로 조회한다")
    void searchAllPublicTrips() {
        Trip first = saveTrip("첫 여행", "검색국가", "첫도시", true);
        Trip second = saveTrip("둘째 여행", "검색국가", "둘째도시", true);
        saveTrip("비공개 여행", "검색국가", "첫도시", false);

        Page<TripSearchResponse> result = search(null, TripSearchScope.ALL, "검색국가", null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(TripSearchResponse::tripId)
            .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("국가와 도시를 지정하면 해당 지역의 여행기만 조회한다")
    void searchByCountryAndCity() {
        Trip expected = saveTrip("도쿄 여행", "Japan", "Tokyo", true);
        saveTrip("오사카 여행", "Japan", "Osaka", true);
        saveTrip("서울 여행", "Korea", "Seoul", true);

        Page<TripSearchResponse> result = search(
            null,
            TripSearchScope.ALL,
            " japan ",
            "TOKYO"
        );

        assertThat(result.getContent())
            .extracting(TripSearchResponse::tripId)
            .containsExactly(expected.getId());
    }

    @Test
    @DisplayName("ALL 범위의 여러 토큰은 여행 제목과 서로 다른 포스트에 나뉘어 있어도 모두 일치한다")
    void searchAllScopeAcrossTripAndPosts() {
        Trip expected = saveTrip("Tokyo 산책", "범위국가", "범위도시", true);
        savePost(expected, LocalDate.of(2026, 1, 1), "Ramen 맛집", "첫 내용");
        savePost(expected, LocalDate.of(2026, 1, 2), "둘째 날", "조용한 Temple 방문");
        savePost(expected, LocalDate.of(2026, 1, 3), "Ramen 추가 기록", "중복 결과 방지");

        Page<TripSearchResponse> result = search(
            "TOKYO ramen temple",
            TripSearchScope.ALL,
            "범위국가",
            null
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
            .extracting(TripSearchResponse::tripId)
            .containsExactly(expected.getId());
    }

    @Test
    @DisplayName("검색 범위에 따라 여행 제목, 포스트 제목, 포스트 내용을 구분한다")
    void searchByScope() {
        Trip trip = saveTrip("TripTitleKeyword", "스코프국가", "스코프도시", true);
        savePost(
            trip,
            LocalDate.of(2026, 1, 1),
            "PostTitleKeyword",
            "PostContentKeyword"
        );

        assertThat(search("TripTitleKeyword", TripSearchScope.TRIP_TITLE, "스코프국가", null))
            .hasSize(1);
        assertThat(search("PostTitleKeyword", TripSearchScope.POST_TITLE, "스코프국가", null))
            .hasSize(1);
        assertThat(search("PostContentKeyword", TripSearchScope.POST_CONTENT, "스코프국가", null))
            .hasSize(1);
        assertThat(search("PostContentKeyword", TripSearchScope.TRIP_TITLE, "스코프국가", null))
            .isEmpty();
    }

    @Test
    @DisplayName("특수문자는 구분자로 처리하고 중복 검색어는 제거한다")
    void normalizeKeyword() {
        Trip expected = saveTrip("alpha beta gamma", "정규화국가", "정규화도시", true);

        Page<TripSearchResponse> result = search(
            " Alpha%beta_beta\\gamma ",
            TripSearchScope.TRIP_TITLE,
            "정규화국가",
            null
        );

        assertThat(result.getContent())
            .extracting(TripSearchResponse::tripId)
            .containsExactly(expected.getId());
    }

    @Test
    @DisplayName("미리보기는 첫 포스트 내용을 공백 정리 후 최대 100자로 반환한다")
    void createPreviewFromFirstPost() {
        Trip trip = saveTrip("미리보기 여행", "미리보기국가", "미리보기도시", true);
        savePost(trip, LocalDate.of(2026, 1, 2), "나중 포스트", "나중 내용");
        savePost(
            trip,
            LocalDate.of(2026, 1, 1),
            "첫 포스트",
            "  첫째 줄\n" + "가".repeat(120) + "  "
        );

        TripSearchResponse result = search(
            null,
            TripSearchScope.ALL,
            "미리보기국가",
            null
        ).getContent().getFirst();

        assertThat(result.previewText())
            .hasSize(100)
            .startsWith("첫째 줄 ")
            .endsWith("...");
    }

    @Test
    @DisplayName("국가 없이 도시만 지정하면 예외가 발생한다")
    void rejectCityWithoutCountry() {
        assertThatThrownBy(
            () -> search(null, TripSearchScope.ALL, null, "Seoul")
        )
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("도시를 검색하려면 국가를 함께 지정해주세요");
    }

    @Test
    @DisplayName("검색 지역은 공개 트립 기준으로 공백과 중복을 제거해 이름순으로 반환한다")
    void findSearchLocations() {
        saveTrip("첫 지역", " 테스트국가가 ", " 테스트도시나 ", true);
        saveTrip("중복 지역", "테스트국가가", "테스트도시나", true);
        saveTrip("둘째 도시", "테스트국가가", "테스트도시다", true);
        saveTrip("둘째 국가", "테스트국가나", "테스트도시가", true);
        saveTrip("비공개 지역", "테스트비공개국가", "테스트비공개도시", false);
        saveTrip("빈 국가", " ", "테스트빈도시", true);
        saveTrip("빈 도시", "테스트빈국가", " ", true);

        List<TripSearchLocationResponse> locations = tripSearchService.findLocations();
        List<TripSearchLocationResponse> testLocations = locations.stream()
            .filter(location -> location.country().startsWith("테스트"))
            .toList();

        assertThat(testLocations)
            .extracting(TripSearchLocationResponse::country)
            .containsExactly("테스트국가가", "테스트국가나");
        assertThat(testLocations.getFirst().cities())
            .containsExactly("테스트도시나", "테스트도시다");
    }

    @Test
    @DisplayName("검색 결과를 최신, 오래된, 좋아요 많은, 좋아요 적은 순으로 정렬한다")
    void sortSearchResults() {
        Trip first = saveTrip("첫 트립", "정렬국가", "정렬도시", true);
        Trip second = saveTrip("둘째 트립", "정렬국가", "정렬도시", true);
        Trip third = saveTrip("셋째 트립", "정렬국가", "정렬도시", true);
        first.increaseLikeCount();
        first.increaseLikeCount();
        third.increaseLikeCount();
        tripRepository.flush();

        assertThat(search(null, TripSearchScope.ALL, "정렬국가", null, TripSearchSort.LATEST))
            .extracting(TripSearchResponse::tripId)
            .containsExactly(third.getId(), second.getId(), first.getId());
        assertThat(search(null, TripSearchScope.ALL, "정렬국가", null, TripSearchSort.OLDEST))
            .extracting(TripSearchResponse::tripId)
            .containsExactly(first.getId(), second.getId(), third.getId());
        assertThat(search(null, TripSearchScope.ALL, "정렬국가", null, TripSearchSort.MOST_LIKED))
            .extracting(TripSearchResponse::tripId)
            .containsExactly(first.getId(), third.getId(), second.getId());
        assertThat(search(null, TripSearchScope.ALL, "정렬국가", null, TripSearchSort.LEAST_LIKED))
            .extracting(TripSearchResponse::tripId)
            .containsExactly(second.getId(), third.getId(), first.getId());
    }

    private Page<TripSearchResponse> search(
        String keyword,
        TripSearchScope scope,
        String country,
        String city
    ) {
        return search(keyword, scope, country, city, TripSearchSort.LATEST);
    }

    private Page<TripSearchResponse> search(
        String keyword,
        TripSearchScope scope,
        String country,
        String city,
        TripSearchSort sort
    ) {
        return tripSearchService.search(
            keyword,
            scope,
            country,
            city,
            sort,
            PageRequest.of(0, 20)
        );
    }

    private Trip saveTrip(
        String title,
        String country,
        String city,
        boolean visibility
    ) {
        return tripRepository.save(
            new Trip(
                owner,
                title,
                country,
                city,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0),
                visibility
            )
        );
    }

    private Post savePost(Trip trip, LocalDate date, String title, String memo) {
        return postRepository.save(new Post(trip, date, title, memo));
    }
}
