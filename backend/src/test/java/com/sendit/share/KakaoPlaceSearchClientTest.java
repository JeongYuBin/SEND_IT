package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class KakaoPlaceSearchClientTest {
    @Test
    void enrichesBothDirectionsThroughHttpSearch() throws Exception {
        var http = org.mockito.Mockito.mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<String> response = org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(response.statusCode()).thenReturn(200);
        org.mockito.Mockito.when(response.body()).thenReturn("""
                {"documents":[{"place_name":"테스트식당","road_address_name":"서울 관악구 보라매로3길 17",
                "category_group_name":"음식점","x":"126.927","y":"37.491"}]}
                """);
        org.mockito.Mockito.when(http.send(org.mockito.ArgumentMatchers.any(java.net.http.HttpRequest.class),
                org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        var lookup = new KakaoPlaceSearchClient(new ObjectMapper(), "test", "https://example.com", http, Duration.ofSeconds(1));
        var addressOnly = lookup.enrich(new PageMetadata("제목", null, null, null, null,
                "서울특별시 관악구 보라매로3길 17", null, null));
        assertThat(addressOnly.placeName()).isEqualTo("테스트식당");
        var nameOnly = lookup.enrich(new PageMetadata("제목", null, null, "테스트식당", null, null, null, null));
        assertThat(nameOnly.address()).isEqualTo("서울 관악구 보라매로3길 17");
        var wrongName = lookup.enrich(new PageMetadata("제목", null, null, "잘못 추출된 제목", null,
                "서울특별시 관악구 보라매로3길 17", null, null));
        assertThat(wrongName.placeName()).isEqualTo("테스트식당");
    }

    @Test
    void doesNotConfuseBuildingNumbersOrChooseAmongMultipleTenants() {
        String body = """
                {"documents":[{"place_name":"A식당","road_address_name":"서울 관악구 보라매로3길 170"}]}
                """;
        assertThat(client.parseAddress(body, "서울 관악구 보라매로3길 17")).isEmpty();
        String multiple = """
                {"documents":[{"place_name":"A식당","road_address_name":"서울 관악구 보라매로3길 17"},
                {"place_name":"B식당","road_address_name":"서울 관악구 보라매로3길 17"}]}
                """;
        assertThat(client.parseAddress(multiple, "서울 관악구 보라매로3길 17")).isEmpty();
    }
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

    @Test
    void rejectsPartialNameWithoutSourceAddress() {
        String response = """
                {"documents": [
                  {"place_name":"스시화 잠실점", "address_name":"서울 송파구 잠실동",
                   "road_address_name":"서울 송파구 백제고분로 1", "x":"127.08", "y":"37.51"}
                ]}
                """;
        PageMetadata fallback = new PageMetadata(
                "영상", "설명", null, "스시화", "음식점", null, null, null);

        assertThat(client.parse(response, fallback)).isEmpty();
    }

    @Test
    void resolvesPlaceNameFromExactAddressWithoutAName() {
        String response = """
                {"documents": [
                  {"place_name":"훈스타포크", "category_group_name":"음식점",
                   "address_name":"서울 관악구 봉천동 1690-153",
                   "road_address_name":"서울 관악구 보라매로3길 17", "x":"126.927", "y":"37.491"},
                  {"place_name":"다른 식당", "category_group_name":"음식점",
                   "road_address_name":"서울 관악구 보라매로3길 19", "x":"126.928", "y":"37.492"}
                ]}
                """;

        PageMetadata result = client.parseAddress(response, "서울 관악구 보라매로3길 17").orElseThrow();

        assertThat(result.placeName()).isEqualTo("훈스타포크");
        assertThat(result.category()).isEqualTo("음식점");
        assertThat(result.address()).isEqualTo("서울 관악구 보라매로3길 17");
        assertThat(result.latitude()).isEqualTo(37.491);
        assertThat(result.longitude()).isEqualTo(126.927);
    }
}
