package com.triptrace.domain.image.image.service

import com.triptrace.domain.image.image.dto.response.ImageServiceResponse
import com.triptrace.domain.image.image.entity.Image
import com.triptrace.domain.image.image.error.ImageErrorCode
import com.triptrace.domain.image.image.mapper.ImageMapper
import com.triptrace.domain.image.image.repository.ImageRepository
import com.triptrace.domain.marker.marker.repository.MarkerRepository
import com.triptrace.domain.member.member.entity.Member
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.domain.trip.trip.repository.TripRepository
import com.triptrace.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

@Service
class ImageService(
    private val imageRepository: ImageRepository,
    private val tripRepository: TripRepository,
    private val markerRepository: MarkerRepository,
) {
    @Transactional
    fun create(image: Image): ImageServiceResponse =
        ImageMapper.toServiceResponse(imageRepository.save(image))

    @Transactional
    fun delete(image: Image): ImageServiceResponse {
        val response = ImageMapper.toServiceResponse(image)
        imageRepository.delete(image)
        return response
    }

    @Transactional
    fun modifyPost(owner: Member, trip: Trip, post: Post, imageId: Long): ImageServiceResponse {
        val image = getById(imageId)
        if (!validateOwner(owner, image)) throw ServiceException(ImageErrorCode.FORBIDDEN)
        if (!validateTrip(trip, image)) throw ServiceException(ImageErrorCode.INVALID_TRIP)
        image.modifyPost(post)
        return ImageMapper.toServiceResponse(image)
    }

    @Transactional
    fun delete(owner: Member, trip: Trip, post: Post?, id: Long): ImageServiceResponse {
        val image = getById(id)
        validate(owner, trip, post, image)
        disconnectRepresentativeReferences(image.id)
        return delete(image)
    }

    @Transactional
    fun delete(owner: Member, trip: Trip, id: Long): ImageServiceResponse {
        val image = getById(id)
        validate(owner, trip, null, image)
        disconnectRepresentativeReferences(image.id)
        return delete(image)
    }

    @Transactional
    fun delete(owner: Member, trip: Trip, post: Post?, imageUrl: String): ImageServiceResponse {
        val image = getByUrl(imageUrl)
        validate(owner, trip, post, image)
        disconnectRepresentativeReferences(image.id)
        return delete(image)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): Image = imageRepository.findByIdOrNull(id)
        ?: throw ServiceException(ImageErrorCode.NOT_FOUND)

    @Transactional(readOnly = true)
    fun getByUrl(originalFileUrl: String): Image =
        imageRepository.findByOriginalFileUrl(originalFileUrl)
            ?: throw ServiceException(ImageErrorCode.NOT_FOUND)

    @Transactional(readOnly = true)
    fun findById(id: Long): ImageServiceResponse = ImageMapper.toServiceResponse(getById(id))

    @Transactional(readOnly = true)
    fun findWithOwner(ownerId: Long): List<ImageServiceResponse> =
        imageRepository.findByOwnerId(ownerId)
            .map(ImageMapper::toServiceResponse)

    @Transactional
    fun unassign(ownerId: Long, tripId: Long, imageId: Long): ImageServiceResponse {
        val image = imageRepository.findByIdAndOwnerIdAndTripId(imageId, ownerId, tripId)
            ?: throw ServiceException(ImageErrorCode.NOT_FOUND)
        image.modifyPost(null)
        return ImageMapper.toServiceResponse(image)
    }

    private fun disconnectRepresentativeReferences(imageId: Long?) {
        if (imageId == null) return
        tripRepository.findByRepresentativeImageId(imageId)
            .forEach { it.changeRepresentativeImage(null) }
        markerRepository.findByRepresentativeImageId(imageId)
            .forEach { it.changeRepresentativeImage(null) }
    }

    private fun validate(owner: Member, trip: Trip, post: Post?, image: Image) {
        if (!validateOwner(owner, image)) throw ServiceException(ImageErrorCode.FORBIDDEN)
        if (!validateTrip(trip, image)) throw ServiceException(ImageErrorCode.INVALID_TRIP)
        if (!validatePost(post, image)) throw ServiceException(ImageErrorCode.INVALID_POST)
    }

    private fun validateOwner(owner: Member, image: Image): Boolean = owner.id == image.owner.id

    private fun validateTrip(trip: Trip, image: Image): Boolean = trip.id == image.trip.id

    private fun validatePost(post: Post?, image: Image): Boolean =
        post == null || (image.post != null && post.id == image.post?.id)
}
