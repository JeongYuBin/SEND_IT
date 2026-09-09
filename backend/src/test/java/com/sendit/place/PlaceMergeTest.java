package com.sendit.place;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceMergeTest {
    @Test
    void mergesKakaoDetailsIntoExistingPlace() {
        Place place = new Place("스시화", null, null, null,
                null, null, "기존 설명", null);

        place.mergeExternalDetails(
                "음식점", "서울 송파구 잠실동 207-16",
                "서울 송파구 백제고분로17길 43", 37.509465, 127.085269,
                null, null, "02-123-4567", "12345",
                "http://place.map.kakao.com/12345");

        assertThat(place.getCategory()).isEqualTo("음식점");
        assertThat(place.getDescription()).isEqualTo("기존 설명");
        assertThat(place.getRoadAddress()).isEqualTo("서울특별시 송파구 백제고분로17길 43");
        assertThat(place.getPhone()).isEqualTo("02-123-4567");
        assertThat(place.getKakaoPlaceId()).isEqualTo("12345");
        assertThat(place.getLatitude()).isEqualTo(37.509465);
    }
}
