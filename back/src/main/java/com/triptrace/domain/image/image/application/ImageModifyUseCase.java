package com.triptrace.domain.image.image.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.triptrace.domain.image.image.error.ImageErrorCode;
import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageModifyUseCase {
    private final ImageService imageService;
    private final TripService tripService;
    private final PostService postService;
    private final MemberService memberService;

    @Transactional
    public ImageServiceResponse modifyById(Long ownerId, Long tripId, Long postId, Long imageId) {
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postService.getPost(trip, postId);
        if (!post.getTrip().getId().equals(trip.getId())) {
            throw new ServiceException(ImageErrorCode.INVALID);
        }
        return imageService.modifyPost(owner, trip, post, imageId);
    }

    public ImageServiceResponse unassign(Long ownerId, Long tripId, Long imageId) {
        ImageServiceResponse imageServiceResponse = imageService.unassign(ownerId,tripId, imageId);
        return imageServiceResponse;

    }
}
