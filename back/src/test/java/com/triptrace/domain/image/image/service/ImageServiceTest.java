package com.triptrace.domain.image.image.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.error.ImageErrorCode;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.entity.MarkerSource;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.global.exception.ServiceException;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ImageServiceTest {
    @Autowired
    private ImageService imageService;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private MarkerRepository markerRepository;

    private Member createMember(String username) {
        return memberRepository.save(new Member(
            "%s@test.com".formatted(username), username, "passwordHash", null, MemberStatus.ACTIVE
        ));
    }

    private Trip createTrip(Member owner) {
        return tripRepository.save(new Trip(
            owner, "교토 여행", "일본", "교토",
            LocalDateTime.of(2024, 4, 1, 0, 0),
            LocalDateTime.of(2024, 4, 5, 0, 0),
            true
        ));
    }

    private Post createPost(Trip trip) {
        return postRepository.save(new Post(
            trip, LocalDate.of(2024, 4, 1), "첫날", "교토에 도착했다."
        ));
    }

    private Image toEntity(Member owner, Trip trip, Post post) {
        return imageRepository.save(new Image(
            owner, trip, post,
            "https://example.com/images/%s.jpg".formatted(java.util.UUID.randomUUID()),
            null, 1024L, "image/jpeg", UploadStatus.STORED
        ));
    }

    @Test
    @DisplayName("post 없이 업로드된 이미지에 post를 지정할 수 있다")
    void modifyPost() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, null);

        ImageServiceResponse response = imageService.modifyPost(owner, trip, post, image.getId());

        assertThat(response.getPostId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("소유자가 아니면 post 지정 시 예외가 발생한다")
    void modifyPostNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, null);

        assertThatThrownBy(() -> imageService.modifyPost(other, trip, post, image.getId()))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("이미지가 속하지 않은 trip으로 post 지정 시 예외가 발생한다")
    void modifyPostWrongTrip() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Trip otherTrip = createTrip(owner);
        Post postInOtherTrip = createPost(otherTrip);
        Image image = toEntity(owner, trip, null);

        assertThatThrownBy(() -> imageService.modifyPost(owner, otherTrip, postInOtherTrip, image.getId()))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("id로 이미지를 삭제하면 더 이상 조회되지 않는다")
    void deleteById() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, post);

        imageService.delete(owner, trip, post, image.getId());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
    }

    @Test
    @DisplayName("url로 이미지를 삭제하면 더 이상 조회되지 않는다")
    void deleteByUrl() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, post);

        imageService.delete(owner, trip, post, image.getOriginalFileUrl());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
    }

    @Test
    @DisplayName("대표이미지로 사용 중인 이미지도 참조를 해제한 뒤 삭제할 수 있다")
    void deleteRepresentativeImage() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, post);
        Marker marker = markerRepository.save(new Marker(
            post,
            java.math.BigDecimal.valueOf(35.0116363),
            java.math.BigDecimal.valueOf(135.7680294),
            "교토역",
            LocalDateTime.of(2024, 4, 1, 12, 0),
            MarkerSource.AUTO,
            image
        ));
        trip.changeRepresentativeImage(image);

        imageService.delete(owner, trip, post, image.getId());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getRepresentativeImage()).isNull();
        assertThat(markerRepository.findById(marker.getId()).orElseThrow().getRepresentativeImage()).isNull();
    }

    @Test
    @DisplayName("post 범위 없이 대표이미지를 삭제해도 참조를 해제한다")
    void deleteRepresentativeImageWithoutPostScope() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, post);
        Marker marker = markerRepository.save(new Marker(
            post,
            java.math.BigDecimal.valueOf(35.0116363),
            java.math.BigDecimal.valueOf(135.7680294),
            "교토역",
            LocalDateTime.of(2024, 4, 1, 12, 0),
            MarkerSource.AUTO,
            image
        ));
        trip.changeRepresentativeImage(image);

        imageService.delete(owner, trip, image.getId());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getRepresentativeImage()).isNull();
        assertThat(markerRepository.findById(marker.getId()).orElseThrow().getRepresentativeImage()).isNull();
    }

    @Test
    @DisplayName("소유자가 아니면 삭제할 수 없다")
    void deleteNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, post);

        assertThatThrownBy(() -> imageService.delete(other, trip, post, image.getId()))
            .isInstanceOf(ServiceException.class);
        assertThat(imageRepository.findById(image.getId())).isPresent();
    }

    @Test
    @DisplayName("이미지가 실제로 속한 post와 다른 post를 지정하면 삭제할 수 없다")
    void deleteWrongPost() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Post anotherPost = createPost(trip);
        Image image = toEntity(owner, trip, post);

        assertThatThrownBy(() -> imageService.delete(owner, trip, anotherPost, image.getId()))
            .isInstanceOf(ServiceException.class);
        assertThat(imageRepository.findById(image.getId())).isPresent();
    }

    @Test
    @DisplayName("post 없이 삭제를 요청하면 post 일치 여부와 무관하게 삭제된다")
    void deleteWithoutPostScope() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, post);

        imageService.delete(owner, trip, null, image.getId());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
    }

    @Test
    @DisplayName("post에 연결되지 않은 이미지를 특정 post로 삭제하려 하면 예외가 발생한다")
    void deleteUnassignedImageWithPostScope() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, null);

        assertThatThrownBy(() -> imageService.delete(owner, trip, post, image.getId()))
            .isInstanceOf(ServiceException.class);
        assertThat(imageRepository.findById(image.getId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 id를 조회하면 예외가 발생한다")
    void getByIdNotFound() {
        assertThatThrownBy(() -> imageService.findById(-1L))
            .isInstanceOf(ServiceException.class);
    }

    // findWithOwner
    // given: 서로 다른 소유자의 이미지가 존재한다.
    // when: 소유자 ID로 이미지를 조회한다.
    // then: 해당 소유자의 이미지만 서비스 응답으로 반환한다.
    @Test
    @DisplayName("소유자 ID로 해당 소유자의 이미지만 조회한다")
    void findWithOwner_returnsOnlyOwnersImages() {
        Member owner = createMember("owner");
        Member anotherOwner = createMember("another-owner");
        Trip trip = createTrip(owner);
        Trip anotherTrip = createTrip(anotherOwner);
        Image image = toEntity(owner, trip, createPost(trip));
        toEntity(anotherOwner, anotherTrip, createPost(anotherTrip));

        var responses = imageService.findWithOwner(owner.getId());

        assertThat(responses)
            .extracting(ImageServiceResponse::getId)
            .containsExactly(image.getId());
        assertThat(responses.getFirst().getOwnerId()).isEqualTo(owner.getId());
    }

    // given: 이미지가 없는 소유자 ID
    // when: 소유자 ID로 이미지를 조회한다.
    // then: 빈 목록을 반환한다.
    @Test
    @DisplayName("이미지가 없는 소유자는 빈 이미지 목록을 반환한다")
    void findWithOwner_returnsEmptyWhenOwnerHasNoImages() {
        Member owner = createMember("owner");

        assertThat(imageService.findWithOwner(owner.getId())).isEmpty();
    }


    // validate
    // given: 이미지와 맞지 않는 trip
    // when: validate했을 때
    // then: INVALID_TRIP을 반환한다.
    @Test
    @DisplayName("이미지와 다른 여행으로 삭제하면 INVALID_TRIP 예외가 발생한다")
    void deleteWithWrongTrip_throwsInvalidTrip() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Trip anotherTrip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, post);

        assertThatThrownBy(() -> imageService.delete(owner, anotherTrip, post, image.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining(ImageErrorCode.INVALID_TRIP.getMessage());
        assertThat(imageRepository.findById(image.getId())).isPresent();
    }

    // given: 이미지와 맞는 trip, post, owner
    // when: validate했을 때
    // then: ServiceException을 반환하지 않는다.

    // unassign
    // given: post를 가지고 있는 이미지와 맞는 ownerId, imageId, tripId
    // when: unassign 때
    // then: post 값이 null이 된다.
    @Test
    @DisplayName("소유자와 여행이 일치하면 이미지의 게시글 연결을 해제한다")
    void unassign_removesPost() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = toEntity(owner, trip, post);

        ImageServiceResponse response = imageService.unassign(owner.getId(), trip.getId(), image.getId());

        assertThat(response.getPostId()).isNull();
        assertThat(imageRepository.findById(image.getId()).orElseThrow().getPost()).isNull();
    }

    // given: 이미지와 맞지 않는 tripId
    // when: unassign 때
    // then: ServiceException(ImageErrorCode.NOT_FOUND)) 반환한다.
    @Test
    @DisplayName("다른 여행 ID로 게시글 연결 해제를 요청하면 NOT_FOUND 예외가 발생한다")
    void unassignWithWrongTrip_throwsNotFound() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Trip anotherTrip = createTrip(owner);
        Image image = toEntity(owner, trip, createPost(trip));

        assertThatThrownBy(() -> imageService.unassign(owner.getId(), anotherTrip.getId(), image.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining(ImageErrorCode.NOT_FOUND.getMessage());
        assertThat(imageRepository.findById(image.getId()).orElseThrow().getPost()).isNotNull();
    }

    // given: 이미지와 맞지 않는 ownerId
    // when: unassign 때
    // then: ServiceException(ImageErrorCode.NOT_FOUND)) 반환한다.
    @Test
    @DisplayName("다른 소유자 ID로 게시글 연결 해제를 요청하면 NOT_FOUND 예외가 발생한다")
    void unassignWithWrongOwner_throwsNotFound() {
        Member owner = createMember("owner");
        Member anotherOwner = createMember("another-owner");
        Trip trip = createTrip(owner);
        Image image = toEntity(owner, trip, createPost(trip));

        assertThatThrownBy(() -> imageService.unassign(anotherOwner.getId(), trip.getId(), image.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining(ImageErrorCode.NOT_FOUND.getMessage());
        assertThat(imageRepository.findById(image.getId()).orElseThrow().getPost()).isNotNull();
    }
}
