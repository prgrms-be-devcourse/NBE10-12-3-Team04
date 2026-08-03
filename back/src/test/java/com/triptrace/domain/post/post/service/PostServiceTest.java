package com.triptrace.domain.post.post.service;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.entity.MarkerSource;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.dto.PostCreateRequest;
import com.triptrace.domain.post.post.dto.PostModifyRequest;
import com.triptrace.domain.post.post.dto.PostResponse;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.error.PostErrorCode;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.global.app.Domain;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PostServiceTest {
    @Autowired
    private PostService postService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MarkerRepository markerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Test
    @DisplayName("소유자는 여행기에 게시물을 생성할 수 있다.")
    void create() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");

        PostResponse response = postService.create(trip.getId(), owner.getId(), new PostCreateRequest(
            LocalDate.of(2026, 1, 2),
            LocalTime.of(9, 30),
            "둘째 날",
            "아라시야마에 갔다."
        ));

        Post found = postRepository.findById(response.id()).orElseThrow();
        Marker marker = markerRepository.findByPostId(response.id()).orElseThrow();
        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(marker.getVisitedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 30));
        assertThat(response.time()).isEqualTo(LocalTime.of(9, 30));
        assertThat(found.getTitle()).isEqualTo("둘째 날");
    }

    @Test
    @DisplayName("게시물을 수동 생성하면 전체 게시물의 첫 날짜와 끝 날짜로 여행 기간을 보정한다.")
    void createRecalculatesTripDateRange() {
        Member owner = createMember("dateRangeOwner");
        Trip trip = createTrip(owner, "기간 보정 여행");

        postService.create(trip.getId(), owner.getId(), new PostCreateRequest(
            LocalDate.of(2026, 4, 5),
            null,
            "마지막 날",
            "마지막 기록"
        ));
        postService.create(trip.getId(), owner.getId(), new PostCreateRequest(
            LocalDate.of(2026, 4, 2),
            null,
            "첫날",
            "첫 기록"
        ));

        Trip foundTrip = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(foundTrip.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 2).atStartOfDay());
        assertThat(foundTrip.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 5).atStartOfDay());
    }

    @Test
    @DisplayName("소유자가 아니면 게시물을 생성할 수 없다.")
    void createByNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "교토 여행");

        assertThatThrownBy(() -> postService.create(trip.getId(), other.getId(), new PostCreateRequest(
            LocalDate.of(2026, 1, 2),
            LocalTime.of(9, 30),
            "둘째 날",
            "아라시야마에 갔다."
        )))
            .isInstanceOf(ServiceException.class)
            .hasMessage("%s-%s : %s".formatted(
                PostErrorCode.FORBIDDEN.getCode(),
                Domain.POST.getCode(),
                PostErrorCode.FORBIDDEN.getMessage()
            ));
    }

    @Test
    @DisplayName("여행기 게시물 목록을 날짜와 시간 오름차순으로 조회한다.")
    void findPostsByTripId() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        createPost(trip, LocalDate.of(2026, 1, 3), "셋째 날");
        createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");
        createPost(trip, LocalDate.of(2026, 1, 2), "둘째 날");
        Post morningPost = postRepository.save(new Post(trip, LocalDate.of(2026, 1, 2), "둘째 날 오전", "memo"));
        markerRepository.save(new Marker(
            morningPost,
            null,
            null,
            null,
            LocalDateTime.of(2026, 1, 2, 8, 30),
            MarkerSource.MANUAL
        ));

        List<PostResponse> responses = postService.findPostsByTripId(trip.getId(), owner.getId());

        assertThat(responses)
            .extracting(PostResponse::title)
            .containsExactly("첫째 날", "둘째 날 오전", "둘째 날", "셋째 날");
    }

    @Test
    @DisplayName("비공개 여행기의 소유자는 게시물 목록을 조회할 수 있다.")
    void findPrivatePostsByOwner() {
        Member owner = createMember("privateListOwner");
        Trip trip = createTrip(owner, "비공개 여행기", false);
        createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        List<PostResponse> responses = postService.findPostsByTripId(trip.getId(), owner.getId());

        assertThat(responses)
            .extracting(PostResponse::title)
            .containsExactly("첫째 날");
    }

    @Test
    @DisplayName("비공개 여행기의 소유자가 아니면 게시물 목록을 조회할 수 없다.")
    void findPrivatePostsByNotOwner() {
        Member owner = createMember("privateListOwner");
        Member other = createMember("privateListOther");
        Trip trip = createTrip(owner, "비공개 여행기", false);

        assertThatThrownBy(() -> postService.findPostsByTripId(trip.getId(), other.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("%s-%s : %s".formatted(
                PostErrorCode.FORBIDDEN.getCode(),
                Domain.POST.getCode(),
                PostErrorCode.FORBIDDEN.getMessage()
            ));
    }

    @Test
    @DisplayName("게시물이 없는 회원의 게시물 목록은 빈 목록이다.")
    void getPostsReturnsEmptyList() {
        Member owner = createMember("emptyPostOwner");

        List<PostResponse> responses = postService.getPosts(owner.getId());

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("공개 여행기의 게시물은 ownerId 없이 상세 조회할 수 있다.")
    void findPublicPost() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "공개 여행기");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        PostResponse response = postService.findAccessiblePost(post.getId(), null);

        assertThat(response.title()).isEqualTo("첫째 날");
    }

    @Test
    @DisplayName("비공개 여행기의 게시물은 소유자만 상세 조회할 수 있다.")
    void findPrivatePostByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "비공개 여행기", false);
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        PostResponse response = postService.findAccessiblePost(post.getId(), owner.getId());

        assertThat(response.title()).isEqualTo("첫째 날");
    }

    @Test
    @DisplayName("소유자가 아니면 비공개 여행기의 게시물을 상세 조회할 수 없다.")
    void findPrivatePostByNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "비공개 여행기", false);
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        assertThatThrownBy(() -> postService.findAccessiblePost(post.getId(), other.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("%s-%s : %s".formatted(
                PostErrorCode.FORBIDDEN.getCode(),
                Domain.POST.getCode(),
                PostErrorCode.FORBIDDEN.getMessage()
            ));
    }

    @Test
    @DisplayName("소유자는 게시물을 수정할 수 있다.")
    void modifyByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "수정 전");

        PostResponse response = postService.modifyPost(post.getId(), owner.getId(), new PostModifyRequest(
            LocalDate.of(2026, 1, 2),
            null,
            "수정 후",
            "수정된 메모"
        ));

        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(response.title()).isEqualTo("수정 후");
        assertThat(response.memo()).isEqualTo("수정된 메모");
    }

    @Test
    @DisplayName("게시물 날짜를 수정하면 여행 기간도 다시 계산한다.")
    void modifyRecalculatesTripDateRange() {
        Member owner = createMember("modifyDateRangeOwner");
        Trip trip = createTrip(owner, "수정 기간 보정 여행");
        Post first = createPost(trip, LocalDate.of(2026, 3, 1), "첫 기록");
        Post last = createPost(trip, LocalDate.of(2026, 3, 5), "마지막 기록");

        postService.modifyPost(last.getId(), owner.getId(), new PostModifyRequest(
            LocalDate.of(2026, 3, 2),
            null,
            "수정된 기록",
            "수정된 메모"
        ));

        assertThat(trip.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1).atStartOfDay());
        assertThat(trip.getEndDate()).isEqualTo(LocalDate.of(2026, 3, 2).atStartOfDay());
        assertThat(first.getDate()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    @DisplayName("게시물 날짜를 수정하면 기존 마커의 방문 날짜도 변경한다.")
    void modifySynchronizesMarkerDate() {
        Member owner = createMember("markerDateOwner");
        Trip trip = createTrip(owner, "마커 날짜 여행");
        Post post = createPost(trip, LocalDate.of(2026, 3, 1), "수정 전");
        markerRepository.save(new Marker(
            post,
            BigDecimal.valueOf(35.0116363),
            BigDecimal.valueOf(135.7680294),
            "교토역",
            LocalDateTime.of(2026, 3, 1, 10, 30),
            MarkerSource.AUTO
        ));

        postService.modifyPost(post.getId(), owner.getId(), new PostModifyRequest(
            LocalDate.of(2026, 3, 4),
            null,
            "수정 후",
            "수정된 메모"
        ));

        Marker marker = markerRepository.findByPostId(post.getId()).orElseThrow();
        assertThat(marker.getVisitedAt()).isEqualTo(LocalDateTime.of(2026, 3, 4, 10, 30));
    }

    @Test
    @DisplayName("소유자는 게시물을 삭제할 수 있다.")
    void deleteByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "삭제할 게시물");

        postService.deletePost(post.getId(), owner.getId());

        assertThat(postRepository.existsById(post.getId())).isFalse();
    }

    @Test
    @DisplayName("게시물을 삭제하면 남은 게시물 기준으로 여행 기간을 다시 계산한다.")
    void deleteRecalculatesTripDateRange() {
        Member owner = createMember("deleteDateRangeOwner");
        Trip trip = createTrip(owner, "삭제 기간 보정 여행");
        Post first = createPost(trip, LocalDate.of(2026, 2, 1), "삭제할 첫 기록");
        createPost(trip, LocalDate.of(2026, 2, 3), "남길 기록");

        postService.deletePost(first.getId(), owner.getId());

        assertThat(trip.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 3).atStartOfDay());
        assertThat(trip.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 3).atStartOfDay());
    }

    @Test
    @DisplayName("게시물 삭제 시 마커를 삭제하고 이미지는 여행기 업로드 상태로 되돌린다.")
    void deleteByOwnerWithMarkerAndImages() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "삭제할 게시물");
        Image image = toEntity(owner, trip, post, "kyoto.jpg");
        Marker marker = markerRepository.save(new Marker(
            post,
            BigDecimal.valueOf(35.0116363),
            BigDecimal.valueOf(135.7680294),
            "교토역",
            LocalDateTime.of(2026, 1, 1, 10, 0),
            MarkerSource.AUTO,
            image
        ));
        trip.changeRepresentativeImage(image);

        postService.deletePost(post.getId(), owner.getId());
        postRepository.flush();

        assertThat(postRepository.existsById(post.getId())).isFalse();
        assertThat(markerRepository.existsById(marker.getId())).isFalse();
        Image foundImage = imageRepository.findById(image.getId()).orElseThrow();
        assertThat(foundImage.getPost()).isNull();
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getRepresentativeImage()).isNull();
    }

    @Test
    @DisplayName("게시물과 연결되지 않은 여행 대표 이미지는 게시물 삭제 후에도 유지한다.")
    void deleteKeepsTripRepresentativeImageNotConnectedToPost() {
        Member owner = createMember("representativeOwner");
        Trip trip = createTrip(owner, "대표 이미지 여행");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "삭제할 게시물");
        Image representativeImage = toEntity(owner, trip, null, "trip-cover.jpg");
        trip.changeRepresentativeImage(representativeImage);

        postService.deletePost(post.getId(), owner.getId());

        Trip foundTrip = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(foundTrip.getRepresentativeImage()).isEqualTo(representativeImage);
    }

    @Test
    @DisplayName("다른 여행기의 게시물은 해당 여행기에서 조회할 수 없다.")
    void getPostFromOtherTrip() {
        Member owner = createMember("otherTripOwner");
        Trip trip = createTrip(owner, "조회 기준 여행");
        Trip otherTrip = createTrip(owner, "다른 여행");
        Post otherPost = createPost(otherTrip, LocalDate.of(2026, 1, 1), "다른 게시물");

        assertThatThrownBy(() -> postService.getPost(trip, otherPost.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("%s-%s : %s".formatted(
                PostErrorCode.NOT_FOUND.getCode(),
                Domain.POST.getCode(),
                PostErrorCode.NOT_FOUND.getMessage()
            ));
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
        return createTrip(owner, title, true);
    }

    private Trip createTrip(Member owner, String title, boolean visibility) {
        return tripRepository.save(new Trip(
            owner,
            title,
            "일본",
            "교토",
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 5, 0, 0),
            visibility
        ));
    }

    private Post createPost(Trip trip, LocalDate date, String title) {
        return postRepository.save(new Post(
            trip,
            date,
            title,
            "교토 여행 메모"
        ));
    }

    private Image toEntity(Member owner, Trip trip, Post post, String fileName) {
        return imageRepository.save(new Image(
            owner,
            trip,
            post,
            "/images/serving/%s".formatted(fileName),
            "/images/thumbnail/%s".formatted(fileName),
            1024L,
            "image/jpeg",
            UploadStatus.STORED
        ));
    }
}
