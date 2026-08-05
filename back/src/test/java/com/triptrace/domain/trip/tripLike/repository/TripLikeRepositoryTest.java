package com.triptrace.domain.trip.tripLike.repository;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.entity.TripLike;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 좋아요 조회·삭제가 회원/여행기 조합 단위로 정확히 동작하는지 확인한다.
 * 조합이 어긋나면 남의 좋아요를 지우거나 중복 좋아요가 통과할 수 있다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class TripLikeRepositoryTest {

    @Autowired
    private TripLikeRepository tripLikeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원과 여행기 조합으로 좋아요 존재 여부를 확인한다.")
    void existsByMemberIdAndTripId() {
        Member member = createMember("liker");
        Trip trip = createTrip(createMember("owner-a"));
        tripLikeRepository.save(new TripLike(member, trip));

        assertThat(tripLikeRepository.existsByMemberIdAndTripId(member.getId(), trip.getId())).isTrue();
    }

    @Test
    @DisplayName("다른 회원이 누른 좋아요는 내 것으로 잡히지 않는다.")
    void existsIsScopedToMember() {
        Member other = createMember("other");
        Member me = createMember("me");
        Trip trip = createTrip(createMember("owner-b"));
        tripLikeRepository.save(new TripLike(other, trip));

        assertThat(tripLikeRepository.existsByMemberIdAndTripId(me.getId(), trip.getId())).isFalse();
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 조합을 조회하면 비어 있다.")
    void findByMemberIdAndTripIdNotFound() {
        Member member = createMember("nobody");
        Trip trip = createTrip(createMember("owner-c"));

        assertThat(tripLikeRepository.findByMemberIdAndTripId(member.getId(), trip.getId())).isEmpty();
    }

    @Test
    @DisplayName("여행기별 좋아요 수를 센다.")
    void countByTripId() {
        Trip trip = createTrip(createMember("owner-d"));
        Trip otherTrip = createTrip(createMember("owner-e"));
        tripLikeRepository.save(new TripLike(createMember("c1"), trip));
        tripLikeRepository.save(new TripLike(createMember("c2"), trip));
        tripLikeRepository.save(new TripLike(createMember("c3"), otherTrip));

        assertThat(tripLikeRepository.countByTripId(trip.getId())).isEqualTo(2);
        assertThat(tripLikeRepository.countByTripId(otherTrip.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("여행기가 지워질 때 그 여행기의 좋아요만 함께 지운다.")
    void deleteByTripId() {
        Trip trip = createTrip(createMember("owner-f"));
        Trip otherTrip = createTrip(createMember("owner-g"));
        tripLikeRepository.save(new TripLike(createMember("d1"), trip));
        tripLikeRepository.save(new TripLike(createMember("d2"), otherTrip));

        tripLikeRepository.deleteByTripId(trip.getId());
        entityManager.flush();

        assertThat(tripLikeRepository.countByTripId(trip.getId())).isZero();
        assertThat(tripLikeRepository.countByTripId(otherTrip.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 회원이 같은 여행기에 좋아요를 두 번 저장할 수 없다.")
    void rejectDuplicateLike() {
        Member member = createMember("dup");
        Trip trip = createTrip(createMember("owner-h"));
        tripLikeRepository.save(new TripLike(member, trip));

        // 애플리케이션에서 걸러도 동시 요청에서는 DB의 복합 UNIQUE가 최종 방어선이다.
        assertThatThrownBy(() -> {
            tripLikeRepository.save(new TripLike(member, trip));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
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

    private Trip createTrip(Member owner) {
        return tripRepository.save(new Trip(
            owner,
            "여행기",
            "일본",
            "교토",
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 5, 0, 0),
            true
        ));
    }
}
