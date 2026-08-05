package com.triptrace.domain.trip.tripLike.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.error.TripLikeErrorCode;
import com.triptrace.global.app.Domain;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TripLikeServiceTest {
    @Autowired
    private TripLikeService tripLikeService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripRepository tripRepository;

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

    @Test
    @DisplayName("중복 좋아요를 repository없이 service단에서만 테스트")
    public void duplicateLikeTest() throws Exception {
        Member member = createMember("member");
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "공개여행기");
        tripLikeService.createLike(member.getId(), trip.getId());
        assertThatThrownBy(() -> tripLikeService.createLike(member.getId(), trip.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("%s-%s : %s".formatted(
                TripLikeErrorCode.ALREADY_LIKED.getCode(),
                Domain.TRIP.getCode(),
                TripLikeErrorCode.ALREADY_LIKED.getMessage()
            ));
    }

    @Test
    @DisplayName("좋아요를 누르면 저장되고 여행기의 좋아요 수가 1 늘어난다.")
    void createLike() {
        Member member = createMember("creator");
        Trip trip = createTrip(createMember("owner1"), "공개여행기");
        long before = trip.getLikeCount();

        tripLikeService.createLike(member.getId(), trip.getId());

        assertThat(tripLikeService.isLiked(member.getId(), trip.getId())).isTrue();
        assertThat(trip.getLikeCount()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("없는 회원으로 좋아요를 누르면 예외가 발생한다.")
    void createLikeMemberNotFound() {
        Trip trip = createTrip(createMember("owner2"), "공개여행기");

        assertThatThrownBy(() -> tripLikeService.createLike(-1L, trip.getId()))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("없는 여행기에 좋아요를 누르면 예외가 발생한다.")
    void createLikeTripNotFound() {
        Member member = createMember("creator2");

        assertThatThrownBy(() -> tripLikeService.createLike(member.getId(), -1L))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("좋아요를 취소하면 기록이 지워지고 좋아요 수가 1 줄어든다.")
    void deleteLike() {
        Member member = createMember("canceler");
        Trip trip = createTrip(createMember("owner3"), "공개여행기");
        tripLikeService.createLike(member.getId(), trip.getId());
        long afterLike = trip.getLikeCount();

        tripLikeService.deleteLike(member.getId(), trip.getId());

        assertThat(tripLikeService.isLiked(member.getId(), trip.getId())).isFalse();
        assertThat(trip.getLikeCount()).isEqualTo(afterLike - 1);
    }

    @Test
    @DisplayName("누르지 않은 좋아요를 취소하면 예외가 발생한다.")
    void deleteLikeNotLiked() {
        Member member = createMember("nonliker");
        Trip trip = createTrip(createMember("owner4"), "공개여행기");

        assertThatThrownBy(() -> tripLikeService.deleteLike(member.getId(), trip.getId()))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("없는 여행기의 좋아요를 취소하면 예외가 발생한다.")
    void deleteLikeTripNotFound() {
        Member member = createMember("canceler2");

        assertThatThrownBy(() -> tripLikeService.deleteLike(member.getId(), -1L))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("다른 회원이 누른 좋아요는 내 좋아요로 보이지 않는다.")
    void isLikedIsScopedToMember() {
        Member other = createMember("other1");
        Member me = createMember("me1");
        Trip trip = createTrip(createMember("owner5"), "공개여행기");
        tripLikeService.createLike(other.getId(), trip.getId());

        assertThat(tripLikeService.isLiked(me.getId(), trip.getId())).isFalse();
    }
}
