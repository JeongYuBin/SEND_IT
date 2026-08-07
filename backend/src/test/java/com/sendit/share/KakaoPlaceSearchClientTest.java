package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class KakaoPlaceSearchClientTest {
    private final KakaoPlaceSearchClient client = new KakaoPlaceSearchClient(
            new ObjectMapper(), "test", "https://example.com",
            HttpClient.newHttpClient(), Duration.ofSeconds(1));

    @Test
    void enrichesExactPlaceWithKakaoAddressAndCoordinates() {
        String response = """
                {
                  "documents": [{
                    "place_name": "낙지본집",
                    "category_group_name": "음식점",
                    "address_name": "경기 평택시 동삭동 705-8",
                    "road_address_name": "경기 평택시 비전2로 123",
                    "x": "127.102030",
                    "y": "37.012340"
                  }, {
                    "place_name": "낙지마을",
                    "category_group_name": "음식점",
                    "address_name": "서울특별시 종로구",
                    "x": "126.9",
                    "y": "37.5"
                  }]
                }
                """;
        PageMetadata fallback = new PageMetadata(
                "Instagram 게시물", "#낙지본집", "image", "낙지본집",
                null, null, null, null);

        PageMetadata result = client.parse(response, fallback).orElseThrow();

        assertThat(result.placeName()).isEqualTo("낙지본집");
        assertThat(result.category()).isEqualTo("음식점");
        assertThat(result.address()).isEqualTo("경기 평택시 비전2로 123");
        assertThat(result.latitude()).isEqualTo(37.012340);
        assertThat(result.longitude()).isEqualTo(127.102030);
    }

    @Test
    void prefersExactPlaceInExpectedAddressRegion() {
        String response = """
                {"documents": [
                  {"place_name":"스시화", "address_name":"서울 강남구 역삼동 1",
                   "road_address_name":"서울 강남구 테헤란로 1", "x":"127.1", "y":"37.5"},
                  {"place_name":"스시화", "address_name":"서울 송파구 잠실동 207-16",
                   "road_address_name":"서울 송파구 백제고분로 187", "x":"127.08", "y":"37.51"}
                ]}
                """;
        PageMetadata fallback = new PageMetadata(
                "영상", "설명", null, "스시화", "음식점",
                "서울 송파구 잠실동 207-16", null, null);

        PageMetadata result = client.parse(response, fallback).orElseThrow();

        assertThat(result.address()).isEqualTo("서울 송파구 백제고분로 187");
        assertThat(result.longitude()).isEqualTo(127.08);
    }
}
