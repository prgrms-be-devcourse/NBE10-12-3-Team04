package com.triptrace.domain.trip.tripAuto.controller;

import com.triptrace.domain.trip.tripAuto.dto.TripAutoRecordResponse;
import com.triptrace.domain.trip.tripAuto.service.TripAutoRecordService;
import com.triptrace.global.app.Domain;
import com.triptrace.global.rsData.RsData;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
public class TripAutoRecordController {
    private static final String CREATED_CODE = "201-" + Domain.TRIP.getCode();
    private final TripAutoRecordService tripAutoRecordService;

    // 클라이언트는 Trip 생성/이미지 업로드가 끝난 뒤 이 API 하나로 자동 기록 생성을 요청한다.
    @PostMapping("/{tripId}/auto-records")
    public RsData<TripAutoRecordResponse> createAutoRecords(
        @PathVariable Long tripId,
        @AuthenticationPrincipal Long memberId
    ) {
        TripAutoRecordResponse response = tripAutoRecordService.createAutoRecords(tripId, memberId);

        return new RsData<>(
            CREATED_CODE,
            "이미지 메타데이터 기반 여행 기록이 자동 생성되었습니다.",
            response
        );
    }

    public TripAutoRecordController(final TripAutoRecordService tripAutoRecordService) {
        this.tripAutoRecordService = tripAutoRecordService;
    }
}
