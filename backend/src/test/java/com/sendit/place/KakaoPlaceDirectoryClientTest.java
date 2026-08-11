package com.sendit.place;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class KakaoPlaceDirectoryClientTest {
    private final KakaoPlaceDirectoryClient client = new KakaoPlaceDirectoryClient(
            new ObjectMapper(), "test", "https://example.com",
            HttpClient.newHttpClient(), Duration.ofSeconds(1));

    @Test
    void parsesKakaoKeywordSearchResult() throws Exception {
        String response = """
                {
                  "meta": {"is_end": false},
                  "documents": [{
                    "id": "12345",
                    "place_name": "스시화",
                    "category_name": "음식점 > 일식 > 초밥",
                    "category_group_name": "음식점",
                    "address_name": "서울 송파구 잠실동 207-16",
                    "road_address_name": "서울 송파구 백제고분로17길 43",
                    "phone": "02-123-4567",
                    "x": "127.085269",
                    "y": "37.509465",
                    "place_url": "http://place.map.kakao.com/12345"
                  }]
                }
                """;

        PlaceSearchDtos.Response result = client.parse(response, 2);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.last()).isFalse();
        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.kakaoPlaceId()).isEqualTo("12345");
            assertThat(place.name()).isEqualTo("스시화");
            assertThat(place.categoryGroup()).isEqualTo("음식점");
            assertThat(place.roadAddress()).isEqualTo("서울 송파구 백제고분로17길 43");
            assertThat(place.latitude()).isEqualTo(37.509465);
            assertThat(place.longitude()).isEqualTo(127.085269);
        });
    }
}
