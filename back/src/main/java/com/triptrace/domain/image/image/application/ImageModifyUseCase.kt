package com.triptrace.domain.image.image.application

import com.triptrace.domain.image.image.dto.response.ImageServiceResponse
import com.triptrace.domain.image.image.error.ImageErrorCode
import com.triptrace.domain.image.image.service.ImageService
import com.triptrace.domain.member.member.service.MemberService
import com.triptrace.domain.post.post.service.PostService
import com.triptrace.domain.trip.trip.service.TripService
import com.triptrace.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImageModifyUseCase(
    private val imageService: ImageService,
    private val tripService: TripService,
    private val postService: PostService,
    private val memberService: MemberService,
) {
    @Transactional
    fun modifyById(ownerId: Long, tripId: Long, postId: Long, imageId: Long): ImageServiceResponse {
        val owner = memberService.findById(ownerId)
        val trip = tripService.findOwnedTrip(tripId, owner.id)
        val post = postService.getPost(trip, postId)
        if (post.trip.id != trip.id) throw ServiceException(ImageErrorCode.INVALID)
        return imageService.modifyPost(owner, trip, post, imageId)
    }

    fun unassign(
        ownerId: Long,
        tripId: Long,
        imageId: Long,
    ) = imageService.unassign(ownerId, tripId, imageId)
}
