package com.triptrace.domain.trip.tripLike.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.entity.TripLike;
import com.triptrace.domain.trip.tripLike.error.TripLikeErrorCode;
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripLikeService {
    public final TripLikeRepository tripLikeRepository;
    private final MemberRepository memberRepository;
    private final TripRepository tripRepository;

    @Transactional
    public void createLike(Long memberId, Long tripId) {
        if (tripLikeRepository.existsByMemberIdAndTripId(memberId, tripId)) {
            throw new ServiceException(TripLikeErrorCode.ALREADY_LIKED);
        }

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ServiceException(TripLikeErrorCode.MEMBER_NOT_FOUND));
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ServiceException(TripLikeErrorCode.TRIP_NOT_FOUND));

        TripLike tripLike = new TripLike(member, trip);

        tripLikeRepository.save(tripLike);
        trip.increaseLikeCount();
    }

    @Transactional
    public void deleteLike(Long memberId, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ServiceException(TripLikeErrorCode.TRIP_NOT_FOUND));

        TripLike tripLike = tripLikeRepository.findByMemberIdAndTripId(memberId, tripId)
            .orElseThrow(() -> new ServiceException(TripLikeErrorCode.NOT_LIKED));

        tripLikeRepository.delete(tripLike);
        trip.decreaseLikeCount();
    }

    public boolean isLiked(Long memberId, Long tripId) {
        return tripLikeRepository.existsByMemberIdAndTripId(memberId, tripId);
    }
}
