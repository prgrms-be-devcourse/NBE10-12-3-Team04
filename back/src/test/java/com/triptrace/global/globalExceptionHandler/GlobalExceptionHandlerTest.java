package com.triptrace.global.globalExceptionHandler;

import com.triptrace.global.error.DefaultErrorCode;
import com.triptrace.global.exception.ServiceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예외 타입별로 어떤 상태코드와 응답 본문이 나가는지 고정한다.
 *
 * 각 도메인 테스트가 간접적으로 몇 가지를 덮고 있지만, 핸들러가 만드는 메시지 형식
 * (필드-코드-메시지 조합, 정렬, 줄바꿈 연결)까지 직접 확인하는 테스트는 없었다.
 * 전체 컨텍스트 없이 advice만 붙여서 확인한다.
 */
class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new TestController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @Test
    @DisplayName("NoSuchElementException은 404와 기본 메시지로 응답한다.")
    void handleNoSuchElement() throws Exception {
        mvc.perform(post("/test/no-such-element"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.resultCode").value(DefaultErrorCode.NOT_FOUND.getCode()))
            .andExpect(jsonPath("$.msg").value(DefaultErrorCode.NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("ServiceException은 resultCode 앞자리를 HTTP 상태코드로 사용한다.")
    void handleServiceException() throws Exception {
        // "409-05"의 409가 상태코드가 되고, 코드/메시지는 그대로 내려간다.
        mvc.perform(post("/test/service-exception"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.resultCode").value("409-05"))
            .andExpect(jsonPath("$.msg").value("이미 존재합니다."));
    }

    @Test
    @DisplayName("검증 실패는 400과 '필드-코드-메시지' 형식으로 응답한다.")
    void handleMethodArgumentNotValid() throws Exception {
        mvc.perform(post("/test/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value(DefaultErrorCode.BAD_REQUEST.getCode()))
            // 뒤에 붙는 문구는 로케일에 따라 달라지므로 '필드-코드-' 조합까지만 고정한다.
            .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.startsWith("name-NotBlank-")));
    }

    @Test
    @DisplayName("본문을 읽을 수 없으면 400과 기본 메시지로 응답한다.")
    void handleHttpMessageNotReadable() throws Exception {
        mvc.perform(post("/test/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value(DefaultErrorCode.BAD_REQUEST.getCode()))
            .andExpect(jsonPath("$.msg").value(DefaultErrorCode.BAD_REQUEST.getMessage()));
    }

    @Test
    @DisplayName("필수 헤더가 없으면 400과 '헤더-NotBlank-메시지' 형식으로 응답한다.")
    void handleMissingRequestHeader() throws Exception {
        mvc.perform(post("/test/header"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value(DefaultErrorCode.BAD_REQUEST.getCode()))
            .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.startsWith("X-Test-Header-NotBlank-")));
    }

    @Test
    @DisplayName("업로드 용량 초과는 413으로 응답한다.")
    void handleMaxUploadSizeExceeded() throws Exception {
        mvc.perform(post("/test/too-large"))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.resultCode").value(DefaultErrorCode.PAYLOAD_TOO_LARGE.getCode()))
            .andExpect(jsonPath("$.msg")
                .value(org.hamcrest.Matchers.startsWith(DefaultErrorCode.PAYLOAD_TOO_LARGE.getMessage())));
    }

    // 예외만 던지는 테스트 전용 컨트롤러. 실제 엔드포인트에 의존하지 않고 핸들러만 검증한다.
    @RestController
    static class TestController {

        @PostMapping("/test/no-such-element")
        void noSuchElement() {
            throw new NoSuchElementException();
        }

        @PostMapping("/test/service-exception")
        void serviceException() {
            throw new ServiceException("409-05", "이미 존재합니다.");
        }

        @PostMapping("/test/valid")
        void valid(@RequestBody @Valid TestRequest request) {
        }

        @PostMapping("/test/header")
        void header(@RequestHeader("X-Test-Header") String value) {
        }

        @PostMapping("/test/too-large")
        void tooLarge() {
            throw new MaxUploadSizeExceededException(1024L);
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
