package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class VisitKoreaMetadataClientTest {

    private final VisitKoreaMetadataClient client =
            new VisitKoreaMetadataClient(new ObjectMapper(), 1, 1);

    @Test
    void mergesTourDetailFieldsIntoHtmlMetadata() throws Exception {
        PageMetadata fallback = new PageMetadata(
                "월래순교자관> 여행지 :대한민국 구석구석",
                "기존 설명",
                "https://example.com/image.jpg",
                "월래순교자관",
                null,
                null,
                null,
                null
        );
        String response = """
                {
                  "body": {
                    "detail": {
                      "title": "월래순교자관",
                      "overView": "만두 달인의 맛집",
                      "cat1Nm": "음식",
                      "cat2Nm": "외국식",
                      "cat3Nm": "중식",
                      "addr1": "서울특별시 구로구 디지털로19길 13 (가리봉동)",
                      "mapY": "37.480275008776104",
                      "mapX": "126.88981439165691"
                    }
                  }
                }
                """;

        PageMetadata result = client.merge(fallback, response);

        assertThat(result.title()).isEqualTo("월래순교자관");
        assertThat(result.placeName()).isEqualTo("월래순교자관");
        assertThat(result.category()).isEqualTo("중식");
        assertThat(result.address()).isEqualTo("서울특별시 구로구 디지털로19길 13 (가리봉동)");
        assertThat(result.latitude()).isEqualTo(37.480275008776104);
        assertThat(result.longitude()).isEqualTo(126.88981439165691);
        assertThat(result.imageUrl()).isEqualTo("https://example.com/image.jpg");
    }
}
