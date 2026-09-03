package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class NaverMapMetadataExtractorTest {
    private final NaverMapMetadataExtractor extractor = new NaverMapMetadataExtractor(
            new ObjectMapper(), HttpClient.newHttpClient(), Duration.ofSeconds(1));

    @Test
    void supportsNaverMapAndShortLinkHostsOnly() {
        assertThat(extractor.supports("https://naver.me/xzHhOyfh")).isTrue();
        assertThat(extractor.supports("https://map.naver.com/p/entry/place/2061717486")).isTrue();
        assertThat(extractor.supports("https://naver.me.evil.example/xzHhOyfh")).isFalse();
    }

    @Test
    void readsPlaceIdFromRedirectPathAndMobileQuery() {
        assertThat(extractor.placeId(URI.create(
                "https://map.naver.com/p/entry/place/2061717486?placePath=%2Fhome")))
                .isEqualTo("2061717486");
        assertThat(extractor.placeId(URI.create(
                "https://m.map.naver.com/?pinId=2061717486&pinType=site")))
                .isEqualTo("2061717486");
    }

    @Test
    void parsesPublicPlaceSummary() {
        PageMetadata result = extractor.parse("""
                {"data":{"placeDetail":{
                  "id":"2061717486",
                  "name":"서울숲 누룽지통닭구이",
                  "businessType":"restaurant",
                  "coordinate":{"longitude":127.0588465,"latitude":37.5408158},
                  "category":{"category":"닭요리"},
                  "address":{"address":"서울 성동구 성수동2가 269-62",
                    "roadAddress":"서울 성동구 성수이로17길 49 1층"},
                  "images":{"images":[{"origin":"https://example.com/store.jpg"}]}
                }}}
                """).orElseThrow();

        assertThat(result.placeName()).isEqualTo("서울숲 누룽지통닭구이");
        assertThat(result.category()).isEqualTo("닭요리");
        assertThat(result.address()).isEqualTo("서울 성동구 성수이로17길 49 1층");
        assertThat(result.latitude()).isEqualTo(37.5408158);
        assertThat(result.longitude()).isEqualTo(127.0588465);
        assertThat(result.imageUrl()).isEqualTo("https://example.com/store.jpg");
    }
}
