package com.triptrace.domain.image.image.application;

import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.error.ImageErrorCode;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageModifyUseCaseTest {

    // given: 소유자·여행·게시글이 모두 일치하는 이미지 수정 요청
    // when: 이미지의 게시글을 변경한다.
    // then: 검증된 객체를 ImageService에 전달하고 수정 응답을 반환한다.
    @Test
    @DisplayName("검증된 소유자와 여행, 게시글로 이미지 게시글을 변경한다")
    void modifyById_delegatesValidatedObjectsToImageService() {
        Fixture fixture = new Fixture();
        ImageServiceResponse expected = response(4L);
        when(fixture.imageService.modifyPost(fixture.owner, fixture.trip, fixture.post, 4L)).thenReturn(expected);

        ImageServiceResponse response = fixture.useCase.modifyById(1L, 2L, 3L, 4L);

        assertThat(response).isSameAs(expected);
        verify(fixture.imageService).modifyPost(fixture.owner, fixture.trip, fixture.post, 4L);
    }

    // given: 요청 여행과 다른 여행에 속한 게시글
    // when: 이미지의 게시글을 변경한다.
    // then: INVALID ServiceException을 던지고 ImageService를 호출하지 않는다.
    @Test
    @DisplayName("게시글이 여행에 속하지 않으면 이미지 수정을 거부한다")
    void modifyById_throwsInvalidWhenPostDoesNotBelongToTrip() {
        Fixture fixture = new Fixture();
        Trip anotherTrip = mock(Trip.class);
        when(anotherTrip.getId()).thenReturn(99L);
        when(fixture.post.getTrip()).thenReturn(anotherTrip);

        ServiceException exception = assertThrows(
            ServiceException.class,
            () -> fixture.useCase.modifyById(1L, 2L, 3L, 4L)
        );

        assertThat(exception.getRsData().resultCode()).isEqualTo(resultCodeOf(ImageErrorCode.INVALID));
    }

    // given: 이미지 연결 해제 요청
    // when: unassign을 호출한다.
    // then: ImageService의 연결 해제 결과를 그대로 반환한다.
    @Test
    @DisplayName("이미지 게시글 연결 해제 결과를 반환한다")
    void unassign_returnsImageServiceResponse() {
        Fixture fixture = new Fixture();
        ImageServiceResponse expected = response(4L);
        when(fixture.imageService.unassign(1L, 2L, 4L)).thenReturn(expected);

        assertThat(fixture.useCase.unassign(1L, 2L, 4L)).isSameAs(expected);
        verify(fixture.imageService).unassign(1L, 2L, 4L);
    }

    private static class Fixture {
        private final ImageService imageService = mock(ImageService.class);
        private final TripService tripService = mock(TripService.class);
        private final PostService postService = mock(PostService.class);
        private final MemberService memberService = mock(MemberService.class);
        private final Member owner = mock(Member.class);
        private final Trip trip = mock(Trip.class);
        private final Post post = mock(Post.class);
        private final ImageModifyUseCase useCase = new ImageModifyUseCase(imageService, tripService, postService, memberService);

        private Fixture() {
            when(owner.getId()).thenReturn(1L);
            when(trip.getId()).thenReturn(2L);
            when(post.getTrip()).thenReturn(trip);
            when(memberService.findById(1L)).thenReturn(owner);
            when(tripService.findOwnedTrip(2L, 1L)).thenReturn(trip);
            when(postService.getPost(trip, 3L)).thenReturn(post);
        }
    }

    private static ImageServiceResponse response(Long imageId) {
        return new ImageServiceResponse(
            imageId, 1L, 2L, 3L, "/images/image.jpg", "/images/image-thumb.jpg", "image/jpeg",
            null, null, null, null, UploadStatus.STORED
        );
    }

    private static String resultCodeOf(ImageErrorCode errorCode) {
        return "%s-%s".formatted(errorCode.getCode(), errorCode.getDomain().getCode());
    }
}
