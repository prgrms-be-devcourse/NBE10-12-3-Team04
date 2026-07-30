package com.triptrace.domain.image.image.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.image.image.storage.ImageFileStorage;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;

@Service
public class ImageDeleteUseCase {
    private final ImageService imageService;
    private final TripService tripService;
    private final PostService postService;
    private final MemberService memberService;
    private final ImageFileStorage imageFileStorage;

    public ImageDeleteUseCase(
        ImageService imageService,
        TripService tripService,
        PostService postService,
        MemberService memberService,
        ImageFileStorage imageFileStorage
    ) {
        this.imageService = imageService;
        this.tripService = tripService;
        this.postService = postService;
        this.memberService = memberService;
        this.imageFileStorage = imageFileStorage;
    }

    @Transactional
    public void deleteByUrl(Long ownerId, Long tripId, Long postId, String imageUrl) {
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postService.getPost(trip, postId);
        ImageServiceResponse imageServiceResponse = imageService.delete(owner, trip, post, imageUrl);
        imageFileStorage.deleteImage(imageServiceResponse.originalFileUrl());
        imageFileStorage.deleteImage(imageServiceResponse.thumbnailUrl());
    }

    @Transactional
    public void deleteById(Long ownerId, Long tripId, Long postId, Long imageId) {
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postService.getPost(trip, postId);
        ImageServiceResponse imageServiceResponse = imageService.delete(owner, trip, post, imageId);
        imageFileStorage.deleteImage(imageServiceResponse.originalFileUrl());
        imageFileStorage.deleteImage(imageServiceResponse.thumbnailUrl());
    }

    @Transactional
    public void deleteById(Long ownerId, Long tripId, Long imageId) {
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        ImageServiceResponse imageServiceResponse = imageService.delete(owner, trip, imageId);
        imageFileStorage.deleteImage(imageServiceResponse.originalFileUrl());
        imageFileStorage.deleteImage(imageServiceResponse.thumbnailUrl());
    }
}
