package com.triptrace.domain.image.image.application;

import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.image.image.storage.ImageFileStorage;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageDeleteUseCaseTest {

    // given: 게시글에 연결된 이미지의 ID 삭제 요청
    // when: 게시글 범위로 이미지를 삭제한다.
    // then: DB 이미지를 삭제하고 원본·섬네일 파일을 순서대로 삭제한다.
    @Test
    @DisplayName("게시글 범위에서 이미지 ID로 삭제하면 원본과 섬네일 파일을 삭제한다")
    void deleteById_withPost_deletesImageAndFiles() {
        Fixture fixture = new Fixture();
        when(fixture.imageService.delete(fixture.owner, fixture.trip, fixture.post, 4L)).thenReturn(response());

        fixture.useCase.deleteById(1L, 2L, 3L, 4L);

        verify(fixture.imageService).delete(fixture.owner, fixture.trip, fixture.post, 4L);
        verifyFilesAreDeletedInOrder(fixture.imageFileStorage);
    }

    // given: 게시글에 연결된 이미지의 URL 삭제 요청
    // when: 게시글 범위로 이미지를 삭제한다.
    // then: URL로 DB 이미지를 삭제하고 원본·섬네일 파일을 삭제한다.
    @Test
    @DisplayName("게시글 범위에서 이미지 URL로 삭제하면 원본과 섬네일 파일을 삭제한다")
    void deleteByUrl_deletesImageAndFiles() {
        Fixture fixture = new Fixture();
        when(fixture.imageService.delete(fixture.owner, fixture.trip, fixture.post, "/images/origin.jpg"))
            .thenReturn(response());

        fixture.useCase.deleteByUrl(1L, 2L, 3L, "/images/origin.jpg");

        verify(fixture.imageService).delete(fixture.owner, fixture.trip, fixture.post, "/images/origin.jpg");
        verifyFilesAreDeletedInOrder(fixture.imageFileStorage);
    }

    // given: 게시글에 연결되지 않은 이미지의 ID 삭제 요청
    // when: 여행 범위로 이미지를 삭제한다.
    // then: DB 이미지를 삭제하고 원본·섬네일 파일을 삭제한다.
    @Test
    @DisplayName("여행 범위에서 이미지 ID로 삭제하면 원본과 섬네일 파일을 삭제한다")
    void deleteById_withoutPost_deletesImageAndFiles() {
        Fixture fixture = new Fixture();
        when(fixture.imageService.delete(fixture.owner, fixture.trip, 4L)).thenReturn(response());

        fixture.useCase.deleteById(1L, 2L, 4L);

        verify(fixture.imageService).delete(fixture.owner, fixture.trip, 4L);
        verifyFilesAreDeletedInOrder(fixture.imageFileStorage);
    }

    private static class Fixture {
        private final ImageService imageService = mock(ImageService.class);
        private final TripService tripService = mock(TripService.class);
        private final PostService postService = mock(PostService.class);
        private final MemberService memberService = mock(MemberService.class);
        private final ImageFileStorage imageFileStorage = mock(ImageFileStorage.class);
        private final Member owner = mock(Member.class);
        private final Trip trip = mock(Trip.class);
        private final Post post = mock(Post.class);
        private final ImageDeleteUseCase useCase = new ImageDeleteUseCase(
            imageService, tripService, postService, memberService, imageFileStorage
        );

        private Fixture() {
            when(owner.getId()).thenReturn(1L);
            when(memberService.findById(1L)).thenReturn(owner);
            when(tripService.findOwnedTrip(2L, 1L)).thenReturn(trip);
            when(postService.getPost(trip, 3L)).thenReturn(post);
        }
    }

    private static ImageServiceResponse response() {
        return new ImageServiceResponse(
            4L, 1L, 2L, 3L, "/images/origin.jpg", "/images/thumb.jpg", "image/jpeg",
            null, null, null, null, UploadStatus.STORED
        );
    }

    private static void verifyFilesAreDeletedInOrder(ImageFileStorage imageFileStorage) {
        InOrder inOrder = inOrder(imageFileStorage);
        inOrder.verify(imageFileStorage).deleteImage("/images/origin.jpg");
        inOrder.verify(imageFileStorage).deleteImage("/images/thumb.jpg");
    }
}
