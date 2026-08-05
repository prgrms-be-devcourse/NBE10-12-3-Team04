package com.triptrace.domain.trip.tripLike.service

import com.triptrace.domain.member.member.repository.MemberRepository
import com.triptrace.domain.trip.trip.repository.TripRepository
import com.triptrace.domain.trip.tripLike.entity.TripLike
import com.triptrace.domain.trip.tripLike.error.TripLikeErrorCode
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository
import com.triptrace.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TripLikeService(
    private val tripLikeRepository: TripLikeRepository,
    private val memberRepository: MemberRepository,
    private val tripRepository: TripRepository
) {

    @Transactional
    fun createLike(memberId: Long, tripId: Long) {
        if (tripLikeRepository.existsByMemberIdAndTripId(memberId, tripId)) {
            throw ServiceException(TripLikeErrorCode.ALREADY_LIKED)
        }

        val member = memberRepository.findById(memberId)
            .orElseThrow { ServiceException(TripLikeErrorCode.MEMBER_NOT_FOUND) }
        val trip = tripRepository.findById(tripId)
            .orElseThrow { ServiceException(TripLikeErrorCode.TRIP_NOT_FOUND) }

        val tripLike = TripLike(member, trip)

        tripLikeRepository.save(tripLike)
        trip.increaseLikeCount()

        log.info("[TRIP] like created tripId: {}, memberId: {}", tripId, memberId)
    }

    @Transactional
    fun deleteLike(memberId: Long, tripId: Long) {
        val trip = tripRepository.findById(tripId)
            .orElseThrow { ServiceException(TripLikeErrorCode.TRIP_NOT_FOUND) }

        val tripLike = tripLikeRepository.findByMemberIdAndTripId(memberId, tripId)
            .orElseThrow { ServiceException(TripLikeErrorCode.NOT_LIKED) }

        tripLikeRepository.delete(tripLike)
        trip.decreaseLikeCount()

        log.info("[TRIP] like deleted tripId: {}, memberId: {}", tripId, memberId)
    }

    fun isLiked(memberId: Long, tripId: Long): Boolean =
        tripLikeRepository.existsByMemberIdAndTripId(memberId, tripId)

    companion object {
        private val log = LoggerFactory.getLogger(TripLikeService::class.java)
    }
}
