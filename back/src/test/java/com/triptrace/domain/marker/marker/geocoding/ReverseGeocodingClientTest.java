package com.triptrace.domain.marker.marker.geocoding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReverseGeocodingClientTest {

    @Test
    @DisplayName("기본 위치 조회는 장소명을 역지오코딩 결과로 변환한다")
    void findLocationUsesPlaceName() {
        BigDecimal latitude = new BigDecimal("37.5665350");
        BigDecimal longitude = new BigDecimal("126.9779692");
        ReverseGeocodingClient client = (requestedLatitude, requestedLongitude) -> {
            assertThat(requestedLatitude).isEqualByComparingTo(latitude);
            assertThat(requestedLongitude).isEqualByComparingTo(longitude);
            return "서울특별시";
        };

        ReverseGeocodingResult result = client.findLocation(latitude, longitude);

        assertThat(result.country()).isNull();
        assertThat(result.city()).isNull();
        assertThat(result.placeName()).isEqualTo("서울특별시");
    }
}
