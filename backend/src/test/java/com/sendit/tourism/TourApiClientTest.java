package com.sendit.tourism;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendit.share.PageMetadata;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class TourApiClientTest {

    @Test
    @SuppressWarnings("unchecked")
    void discoveryUsesNationalPaginationAndCachesPages() throws Exception {
        var http = org.mockito.Mockito.mock(HttpClient.class);
        java.net.http.HttpResponse<String> response = org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(response.statusCode()).thenReturn(200);
        org.mockito.Mockito.when(response.body()).thenReturn("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
                {"contentid":"1","contenttypeid":"12","title":"관광지","mapx":"128","mapy":"37"},
                {"contentid":"2","title":"좌표 없음"}]}}}}
                """);
        org.mockito.Mockito.when(http.send(org.mockito.ArgumentMatchers.any(java.net.http.HttpRequest.class),
                org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        var api = new TourApiClient(new ObjectMapper(), "test", "https://example.com", http, Duration.ofSeconds(1));
        var month = java.time.LocalDate.of(2026, 9, 12);
        var result = api.discover("nearby", month, 2);
        assertThat(result.places()).hasSize(1);
        assertThat(result.hasMore()).isFalse();
        assertThat(api.discover("nearby", month, 2)).isSameAs(result);
        var request = org.mockito.ArgumentCaptor.forClass(java.net.http.HttpRequest.class);
        org.mockito.Mockito.verify(http).send(request.capture(), org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any());
        assertThat(request.getValue().uri().toString()).contains("areaBasedList2", "pageNo=2", "contentTypeId=12");
        api.discover("festival", month, 1);
        org.mockito.Mockito.verify(http, org.mockito.Mockito.times(2)).send(request.capture(), org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any());
        assertThat(request.getValue().uri().toString()).contains("searchFestival2", "eventStartDate=20260901", "eventEndDate=20260930");
    }

    @Test
    void discoveryReportsMissingConfigurationInsteadOfAnEmptyMap() {
        var api = new TourApiClient(new ObjectMapper(), "", "https://example.com", HttpClient.newHttpClient(), Duration.ofSeconds(1));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> api.discover("nearby", java.time.LocalDate.now(), 1))
                .isInstanceOf(IllegalStateException.class);
    }

    private final TourApiClient client = new TourApiClient(
            new ObjectMapper(),
            "test-key",
            "https://example.com",
            HttpClient.newHttpClient(),
            Duration.ofSeconds(1)
    );

    @Test
    void selectsMatchingPlaceAndNormalizesTourInformation() throws Exception {
        String response = """
                {
                  "response": {
                    "header": {"resultCode": "0000", "resultMsg": "OK"},
                    "body": {
                      "items": {
                        "item": [
                          {
                            "contentid": "1",
                            "contenttypeid": "12",
                            "title": "성산일출봉",
                            "addr1": "제주특별자치도 서귀포시",
                            "mapy": "33.4581",
                            "mapx": "126.9425",
                            "firstimage": "https://example.com/seongsan.jpg"
                          },
                          {
                            "contentid": "2",
                            "contenttypeid": "39",
                            "title": "성산식당",
                            "addr1": "제주특별자치도 제주시"
                          }
                        ]
                      }
                    }
                  }
                }
                """;
        PageMetadata fallback = new PageMetadata(
                "성산일출봉 제주 여행",
                "기존 설명",
                null,
                "성산일출봉",
                null,
                "제주특별자치도 서귀포시",
                null,
                null
        );

        List<TourApiClient.TourItem> items = client.items(response);
        TourApiClient.TourItem match = client.bestMatch(fallback, items);
        PageMetadata result = client.merge(fallback, match, match);

        assertThat(match.contentId()).isEqualTo("1");
        assertThat(result.placeName()).isEqualTo("성산일출봉");
        assertThat(result.category()).isEqualTo("관광지");
        assertThat(result.address()).isEqualTo("제주특별자치도 서귀포시");
        assertThat(result.latitude()).isEqualTo(33.4581);
        assertThat(result.longitude()).isEqualTo(126.9425);
        assertThat(result.imageUrl()).isEqualTo("https://example.com/seongsan.jpg");
    }

    @Test
    void rejectsUnrelatedSearchResult() throws Exception {
        String response = """
                {
                  "response": {
                    "header": {"resultCode": "0000"},
                    "body": {
                      "items": {
                        "item": {
                          "contentid": "9",
                          "contenttypeid": "12",
                          "title": "한라산",
                          "addr1": "제주특별자치도 제주시"
                        }
                      }
                    }
                  }
                }
                """;
        PageMetadata fallback = new PageMetadata(
                "성산일출봉", null, null, "성산일출봉",
                null, null, null, null
        );

        assertThat(client.bestMatch(fallback, client.items(response))).isNull();
    }

    @Test
    void usesFallbackWhenTourApiIsDisabled() {
        TourApiClient disabledClient = new TourApiClient(
                new ObjectMapper(),
                "",
                "https://example.com",
                HttpClient.newHttpClient(),
                Duration.ofSeconds(1)
        );
        PageMetadata fallback = new PageMetadata(
                "제목", "설명", null, "장소", null, null, null, null
        );

        assertThat(disabledClient.enrich(fallback)).isSameAs(fallback);
    }

    @Test
    void parsesPetTravelInformation() throws Exception {
        String response = """
                {
                  "response": {
                    "header": {"resultCode": "0000"},
                    "body": {
                      "items": {
                        "item": [{
                          "contentid": "2930334",
                          "acmpyTypeCd": "일부구역 동반가능",
                          "acmpyPsblCpam": "동물 등록을 완료한 전 견종",
                          "acmpyNeedMtr": "목줄 착용",
                          "etcAcmpyInfo": "입·퇴장 시 목줄 착용 필수",
                          "relaPosesFclty": "반려견 놀이터, 공중화장실",
                          "relaAcdntRiskMtr": "CCTV 7개"
                        }]
                      }
                    }
                  }
                }
                """;

        TourApiClient.PetTravelInfo info = client.parsePetInfo(response).orElseThrow();

        assertThat(info.contentId()).isEqualTo("2930334");
        assertThat(info.companionType()).isEqualTo("일부구역 동반가능");
        assertThat(info.allowedPets()).contains("전 견종");
        assertThat(info.requiredItems()).isEqualTo("목줄 착용");
        assertThat(info.facilities()).contains("반려견 놀이터");
        assertThat(info.safetyInformation()).isEqualTo("CCTV 7개");
    }
}
