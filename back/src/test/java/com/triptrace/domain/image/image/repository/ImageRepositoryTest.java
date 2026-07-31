package com.triptrace.domain.image.image.repository;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ImageRepositoryTest {

    @Autowired private ImageRepository imageRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private PostRepository postRepository;

    private Member owner;
    private Trip trip;
    private Post post;
    private Post anotherPost;
    private Image image;
    private Image anotherImage;
    private Image thirdImage;

    @BeforeEach
    void setUp() {
        owner = memberRepository.save(new Member("user@example.com", "traveler", "passwordHash", null, MemberStatus.ACTIVE));
        trip = tripRepository.save(new Trip(
            owner, "교토 여행", "일본", "교토",
            LocalDateTime.of(2024, 4, 1, 0, 0), LocalDateTime.of(2024, 4, 5, 0, 0), true
        ));
        post = postRepository.save(new Post(trip, LocalDate.of(2024, 4, 1), "첫날, 교토 도착", "교토에 도착했다."));
        anotherPost = postRepository.save(new Post(trip, LocalDate.of(2024, 4, 1), "저녁 기록", "저녁을 먹었다."));
        image = saveImage(post, "https://example.com/images/kyoto.jpg");
        anotherImage = saveImage(post, "https://example.com/images/kyoto-temple.jpg");
        thirdImage = saveImage(anotherPost, "https://example.com/images/dinner.jpg");
    }

    // trip id로 이미지 데이터 다건 조회: 단건, 다건, 미존재
    @Test @DisplayName("여행 ID로 이미지 한 건을 조회한다")
    void findByTripId_returnsSingleImage() {
        imageRepository.delete(anotherImage);
        imageRepository.delete(thirdImage);

        assertThat(imageRepository.findByTripId(trip.getId())).containsExactly(image);
    }

    @Test @DisplayName("여행 ID로 이미지 여러 건을 조회한다")
    void findByTripId_returnsMultipleImages() {
        assertThat(imageRepository.findByTripId(trip.getId())).containsExactlyInAnyOrder(image, anotherImage, thirdImage);
    }

    @Test @DisplayName("존재하지 않는 여행 ID로 조회하면 빈 목록을 반환한다")
    void findByTripId_returnsEmptyWhenTripDoesNotExist() {
        assertThat(imageRepository.findByTripId(-1L)).isEmpty();
    }

    // post id로 이미지 데이터 다건 조회: 단건, 다건, 미존재
    @Test @DisplayName("게시글 ID로 이미지 한 건을 조회한다")
    void findByPostId_returnsSingleImage() {
        imageRepository.delete(anotherImage);

        assertThat(imageRepository.findByPostId(post.getId())).containsExactly(image);
    }

    @Test @DisplayName("게시글 ID로 이미지 여러 건을 조회한다")
    void findByPostId_returnsMultipleImages() {
        assertThat(imageRepository.findByPostId(post.getId())).containsExactlyInAnyOrder(image, anotherImage);
    }

    @Test @DisplayName("존재하지 않는 게시글 ID로 조회하면 빈 목록을 반환한다")
    void findByPostId_returnsEmptyWhenPostDoesNotExist() {
        assertThat(imageRepository.findByPostId(-1L)).isEmpty();
    }

    // postIds로 이미지 데이터 다건 조회: 10개 ID 중 단건, 다건, 미존재
    @Test @DisplayName("10개의 게시글 ID 중 일치하는 이미지 한 건을 조회한다")
    void findByPostIdIn_returnsSingleImageFromTenIds() {
        assertThat(imageRepository.findByPostIdIn(idsIncluding(anotherPost.getId())))
            .containsExactly(thirdImage);
    }

    @Test @DisplayName("10개의 게시글 ID 중 일치하는 이미지 여러 건을 조회한다")
    void findByPostIdIn_returnsMultipleImagesFromTenIds() {
        assertThat(imageRepository.findByPostIdIn(idsIncluding(post.getId(), anotherPost.getId())))
            .containsExactlyInAnyOrder(image, anotherImage, thirdImage);
    }

    @Test @DisplayName("존재하지 않는 게시글 ID 목록으로 조회하면 빈 목록을 반환한다")
    void findByPostIdIn_returnsEmptyWhenPostsDoNotExist() {
        assertThat(imageRepository.findByPostIdIn(List.of(-1L, -2L))).isEmpty();
    }

    // owner id로 이미지 데이터 다건 조회: 단건, 다건, 미존재
    @Test @DisplayName("소유자 ID로 이미지 한 건을 조회한다")
    void findByOwnerId_returnsSingleImage() {
        imageRepository.delete(anotherImage);
        imageRepository.delete(thirdImage);

        assertThat(imageRepository.findByOwnerId(owner.getId())).containsExactly(image);
    }

    @Test @DisplayName("소유자 ID로 이미지 여러 건을 조회한다")
    void findByOwnerId_returnsMultipleImages() {
        assertThat(imageRepository.findByOwnerId(owner.getId())).containsExactlyInAnyOrder(image, anotherImage, thirdImage);
    }

    @Test @DisplayName("존재하지 않는 소유자 ID로 조회하면 빈 목록을 반환한다")
    void findByOwnerId_returnsEmptyWhenOwnerDoesNotExist() {
        assertThat(imageRepository.findByOwnerId(-1L)).isEmpty();
    }

    // originalFileUrl로 이미지 데이터 조회: 단건, 미존재
    @Test @DisplayName("원본 파일 URL로 이미지를 조회한다")
    void findByOriginalFileUrl_returnsImage() {
        assertThat(imageRepository.findByOriginalFileUrl(image.getOriginalFileUrl())).contains(image);
    }

    @Test @DisplayName("존재하지 않는 원본 파일 URL로 조회하면 빈 Optional을 반환한다")
    void findByOriginalFileUrl_returnsEmptyWhenUrlDoesNotExist() {
        assertThat(imageRepository.findByOriginalFileUrl("https://example.com/images/missing.jpg")).isEmpty();
    }

    // trip으로 이미지 데이터 다건 조회: 단건, 다건, 미존재
    @Test @DisplayName("여행으로 이미지 한 건을 조회한다")
    void trip_returnsSingleImage() {
        imageRepository.delete(anotherImage);
        imageRepository.delete(thirdImage);

        assertThat(imageRepository.trip(trip)).containsExactly(image);
    }

    @Test @DisplayName("여행으로 이미지 여러 건을 조회한다")
    void trip_returnsMultipleImages() {
        assertThat(imageRepository.trip(trip)).containsExactlyInAnyOrder(image, anotherImage, thirdImage);
    }

    @Test @DisplayName("이미지가 없는 여행으로 조회하면 빈 목록을 반환한다")
    void trip_returnsEmptyWhenTripHasNoImages() {
        Trip emptyTrip = tripRepository.save(new Trip(
            owner, "오사카 여행", "일본", "오사카",
            LocalDateTime.of(2024, 5, 1, 0, 0), LocalDateTime.of(2024, 5, 5, 0, 0), true
        ));

        assertThat(imageRepository.trip(emptyTrip)).isEmpty();
    }

    // findByIdAndOwnerIdAndTripId(@Param("imageId")Long id,@Param("ownerId") Long OwnerId,@Param("tripId") Long TripId)
    // image id, owner id, trip id 가 전부 같아야 값을 보낸다.
    // 하나라도 다르면 값을 보내지 않는다. (3가지 테스트)
    @Test @DisplayName("이미지 ID, 소유자 ID, 여행 ID가 모두 일치하면 이미지를 조회한다")
    void findByIdAndOwnerIdAndTripId_returnsImageWhenAllIdsMatch() {
        assertThat(imageRepository.findByIdAndOwnerIdAndTripId(image.getId(), owner.getId(), trip.getId()))
            .contains(image);
    }

    @Test @DisplayName("이미지 ID가 다르면 이미지를 조회하지 않는다")
    void findByIdAndOwnerIdAndTripId_returnsEmptyWhenImageIdDiffers() {
        assertThat(imageRepository.findByIdAndOwnerIdAndTripId(-1L, owner.getId(), trip.getId())).isEmpty();
    }

    @Test @DisplayName("소유자 ID가 다르면 이미지를 조회하지 않는다")
    void findByIdAndOwnerIdAndTripId_returnsEmptyWhenOwnerIdDiffers() {
        Member anotherOwner = memberRepository.save(new Member(
            "another@example.com", "another", "passwordHash", null, MemberStatus.ACTIVE
        ));

        assertThat(imageRepository.findByIdAndOwnerIdAndTripId(image.getId(), anotherOwner.getId(), trip.getId())).isEmpty();
    }

    @Test @DisplayName("여행 ID가 다르면 이미지를 조회하지 않는다")
    void findByIdAndOwnerIdAndTripId_returnsEmptyWhenTripIdDiffers() {
        Trip anotherTrip = tripRepository.save(new Trip(
            owner, "오사카 여행", "일본", "오사카",
            LocalDateTime.of(2024, 5, 1, 0, 0), LocalDateTime.of(2024, 5, 5, 0, 0), true
        ));

        assertThat(imageRepository.findByIdAndOwnerIdAndTripId(image.getId(), owner.getId(), anotherTrip.getId())).isEmpty();
    }

    // 다른 여행/다른 소유자의 이미지가 결과에 섞이지 않는가
    @Test @DisplayName("다른 소유자와 여행의 이미지는 조회 결과에 섞이지 않는다")
    void findByTripIdAndOwnerId_excludesImagesFromAnotherOwnerAndTrip() {
        Member anotherOwner = memberRepository.save(new Member(
            "another@example.com", "another", "passwordHash", null, MemberStatus.ACTIVE
        ));
        Trip anotherTrip = tripRepository.save(new Trip(
            anotherOwner, "도쿄 여행", "일본", "도쿄",
            LocalDateTime.of(2024, 6, 1, 0, 0), LocalDateTime.of(2024, 6, 5, 0, 0), true
        ));
        Post anotherOwnersPost = postRepository.save(new Post(
            anotherTrip, LocalDate.of(2024, 6, 1), "도쿄 도착", "도쿄에 도착했다."
        ));
        Image anotherOwnersImage = imageRepository.save(new Image(
            anotherOwner, anotherTrip, anotherOwnersPost, "https://example.com/images/tokyo.jpg",
            null, 1024L, "image/jpeg", UploadStatus.STORED
        ));

        assertThat(imageRepository.findByTripId(trip.getId())).doesNotContain(anotherOwnersImage);
        assertThat(imageRepository.findByOwnerId(owner.getId())).doesNotContain(anotherOwnersImage);
    }

    // post == null인 이미지
    @Test @DisplayName("게시글이 연결되지 않은 이미지는 여행과 소유자 조회에는 포함되고 게시글 조회에는 포함되지 않는다")
    void unassignedImage_isExcludedFromPostLookup() {
        Image unassignedImage = saveImage(null, "https://example.com/images/unassigned.jpg");

        assertThat(imageRepository.findByTripId(trip.getId())).contains(unassignedImage);
        assertThat(imageRepository.findByOwnerId(owner.getId())).contains(unassignedImage);
        assertThat(imageRepository.findByPostId(post.getId())).doesNotContain(unassignedImage);
    }

    // URL 중복 정책
    private Image saveImage(Post targetPost, String originalFileUrl) {
        return imageRepository.save(new Image(
            owner, trip, targetPost, originalFileUrl, null, 1024L, "image/jpeg", UploadStatus.STORED
        ));
    }

    private List<Long> idsIncluding(Long... matchedIds) {
        return List.of(matchedIds[0], matchedIds.length > 1 ? matchedIds[1] : -1L,
            -2L, -3L, -4L, -5L, -6L, -7L, -8L, -9L);
    }
}
