package com.triptrace.domain.image.image.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.triptrace.domain.image.image.application.ImageDeleteUseCase;
import com.triptrace.domain.image.image.application.ImageModifyUseCase;
import com.triptrace.domain.image.image.application.ImageSearchUseCase;
import com.triptrace.domain.image.image.application.ImageUploadUseCase;
import com.triptrace.domain.image.image.dto.response.ImageResponse;
import com.triptrace.domain.image.image.dto.response.ImageUploadResponse;
import com.triptrace.global.rsData.RsData;

@RestController
@RequestMapping("/api/v1")
public class ApiV1ImageController {
    private final ImageUploadUseCase imageUploadUseCase;
    private final ImageDeleteUseCase imageDeleteUseCase;
    private final ImageModifyUseCase imageModifyUseCase;
    private final ImageSearchUseCase imageSearchUseCase;

    @PostMapping("/trips/{tripId}/images")
    public RsData<List<ImageUploadResponse>> upload(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long tripId,
        @RequestParam MultipartFile[] images
    ) {
        return new RsData<>(
            "200-1",
            "업로드 되었습니다.",
            imageUploadUseCase.uploadImages(ownerId, tripId, images)
        );
    }

    @PostMapping("/trips/{tripId}/posts/{postId}/images")
    public RsData<List<ImageUploadResponse>> upload(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long tripId,
        @PathVariable Long postId,
        @RequestParam MultipartFile[] images
    ) {
        return new RsData<>(
            "200-1",
            "업로드 되었습니다.",
            imageUploadUseCase.uploadImages(ownerId, tripId, postId, images)
        );
    }
    @DeleteMapping("/trips/{tripId}/posts/{postId}/images/{imageId}")
    public RsData<?> delete(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long tripId,
        @PathVariable Long postId,
        @PathVariable Long imageId
    ) {
        imageDeleteUseCase.deleteById(ownerId, tripId, postId, imageId);
        return new RsData<>(
            "200-1",
            "삭제 되었습니다.",
            null
        );
    }

    @DeleteMapping("/trips/{tripId}/images/{imageId}")
    public RsData<?> delete(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long tripId,
        @PathVariable Long imageId
    ) {
        imageDeleteUseCase.deleteById(ownerId, tripId, imageId);
        return new RsData<>(
            "200-1",
            "삭제 되었습니다.",
            null
        );
    }

    @DeleteMapping("/trips/{tripId}/posts/{postId}/images")
    public RsData<?> delete(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long tripId,
        @PathVariable Long postId,
        @RequestParam String imageUrl
    ) {
        imageDeleteUseCase.deleteByUrl(ownerId, tripId, postId, imageUrl);
        return new RsData<>(
            "200-1",
            "삭제 되었습니다.",
            null
        );
    }
    @PatchMapping("/trips/{tripId}/images")
    public RsData<?> modify(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long tripId,
        @RequestParam Long postId,
        @RequestParam Long imageId
    ) {

        return new RsData<>(
            "200-1",
            "수정 되었습니다.",
            imageModifyUseCase.modifyById(ownerId, tripId, postId, imageId)
        );
    }


    @GetMapping("/images")
    public RsData<List<ImageResponse>> list(
        @AuthenticationPrincipal Long ownerId) {
        return new RsData<>(
            "200-1",
            "SUCCESS",
            imageSearchUseCase.getImages(ownerId));
    }

    @PatchMapping("/trips/{tripId}/images/{imageId}/unassign")
    public RsData<?> unassign(@AuthenticationPrincipal Long ownerId, @PathVariable Long tripId, @PathVariable Long imageId) {
        return new RsData<>(
            "200-1",
            "수정 되었습니다.",
            imageModifyUseCase.unassign(ownerId, tripId, imageId));
    }

    public ApiV1ImageController(final ImageUploadUseCase imageUploadUseCase, final ImageDeleteUseCase imageDeleteUseCase, final ImageModifyUseCase imageModifyUseCase, final ImageSearchUseCase imageSearchUseCase) {
        this.imageUploadUseCase = imageUploadUseCase;
        this.imageDeleteUseCase = imageDeleteUseCase;
        this.imageModifyUseCase = imageModifyUseCase;
        this.imageSearchUseCase = imageSearchUseCase;
    }
}
