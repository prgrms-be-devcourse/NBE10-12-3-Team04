package com.triptrace.domain.image.image.application

import com.triptrace.domain.image.image.service.ImageService
import com.triptrace.domain.image.image.storage.ImageFileStorage
import com.triptrace.domain.member.member.service.MemberService
import com.triptrace.domain.post.post.service.PostService
import com.triptrace.domain.trip.trip.service.TripService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImageDeleteUseCase(private val imageService: ImageService, private val tripService: TripService, private val postService: PostService, private val memberService: MemberService, private val imageFileStorage: ImageFileStorage) {
    @Transactional fun deleteByUrl(ownerId: Long, tripId: Long, postId: Long, imageUrl: String) = delete(ownerId, tripId, postId) { owner, trip, post -> imageService.delete(owner, trip, post, imageUrl) }
    @Transactional fun deleteById(ownerId: Long, tripId: Long, postId: Long, imageId: Long) = delete(ownerId, tripId, postId) { owner, trip, post -> imageService.delete(owner, trip, post, imageId) }
    @Transactional fun deleteById(ownerId: Long, tripId: Long, imageId: Long) {
        val owner = memberService.findById(ownerId); val trip = tripService.findOwnedTrip(tripId, owner.id); val image = imageService.delete(owner, trip, imageId)
        imageFileStorage.deleteImage(image.originalFileUrl()); imageFileStorage.deleteImage(image.thumbnailUrl())
    }
    private fun delete(ownerId: Long, tripId: Long, postId: Long, action: (com.triptrace.domain.member.member.entity.Member, com.triptrace.domain.trip.trip.entity.Trip, com.triptrace.domain.post.post.entity.Post) -> com.triptrace.domain.image.image.dto.response.ImageServiceResponse) {
        val owner = memberService.findById(ownerId); val trip = tripService.findOwnedTrip(tripId, owner.id); val post = postService.getPost(trip, postId); val image = action(owner, trip, post)
        imageFileStorage.deleteImage(image.originalFileUrl()); imageFileStorage.deleteImage(image.thumbnailUrl())
    }
}
