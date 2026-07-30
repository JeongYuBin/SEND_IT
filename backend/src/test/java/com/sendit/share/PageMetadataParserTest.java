package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PageMetadataParserTest {

    private final PageMetadataParser parser = new PageMetadataParser(new ObjectMapper());

    @Test
    void prefersOpenGraphMetadataAndResolvesImageUrl() {
        String html = """
                <html>
                  <head>
                    <title>Fallback title</title>
                    <meta property="og:title" content="서울 여행">
                    <meta property="og:description" content="서울의 하루">
                    <meta property="og:image" content="/images/seoul.jpg">
                  </head>
                </html>
                """;

        PageMetadata result = parser.parse(html, "https://example.com/travel");

        assertThat(result.title()).isEqualTo("서울 여행");
        assertThat(result.description()).isEqualTo("서울의 하루");
        assertThat(result.imageUrl()).isEqualTo("https://example.com/images/seoul.jpg");
    }

    @Test
    void extractsPlaceCandidateFromJsonLd() {
        String html = """
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@type": "TouristAttraction",
                  "name": "경복궁",
                  "address": {
                    "@type": "PostalAddress",
                    "addressRegion": "서울특별시",
                    "addressLocality": "종로구",
                    "streetAddress": "사직로 161"
                  },
                  "geo": {
                    "@type": "GeoCoordinates",
                    "latitude": 37.5796,
                    "longitude": 126.9770
                  }
                }
                </script>
                """;

        PageMetadata result = parser.parse(html, "https://example.com/place");

        assertThat(result.placeName()).isEqualTo("경복궁");
        assertThat(result.category()).isEqualTo("TouristAttraction");
        assertThat(result.address()).contains("서울특별시", "종로구", "사직로 161");
        assertThat(result.latitude()).isEqualTo(37.5796);
        assertThat(result.longitude()).isEqualTo(126.9770);
    }

    @Test
    void cleansVisitKoreaSiteSuffixFromPlaceName() {
        String html = """
                <title>월래순교자관&gt; 여행지 :대한민국 구석구석 </title>
                <meta property="og:title" content="월래순교자관&gt; 여행지 :대한민국 구석구석 ">
                """;

        PageMetadata result = parser.parse(
                html,
                "https://korean.visitkorea.or.kr/detail/ms_detail.do?cotid=test"
        );

        assertThat(result.placeName()).isEqualTo("월래순교자관");
    }
    @Test
    void extractsPlaceFromNaverBlogMapBlock() {
        String html = """
                <meta property="og:title" content="[강릉 맛집] 건도리횟집">
                <meta property="og:description" content="강릉 여행 중 방문한 횟집">
                <meta property="og:image" content="https://example.com/store.jpg">
                <a data-linktype="map"
                   data-linkdata='{"name":"건도리횟집","address":"강원특별자치도 강릉시 창해로 427 건도리횟집","latitude":"37.799404","longitude":"128.913725"}'>
                </a>
                """;

        PageMetadata result = parser.parse(
                html,
                "https://blog.naver.com/PostView.naver?blogId=silver3358&logNo=224296787264"
        );

        assertThat(result.title()).isEqualTo("[강릉 맛집] 건도리횟집");
        assertThat(result.placeName()).isEqualTo("건도리횟집");
        assertThat(result.category()).isEqualTo("음식점");
        assertThat(result.address()).isEqualTo("강원특별자치도 강릉시 창해로 427 건도리횟집");
        assertThat(result.latitude()).isEqualTo(37.799404);
        assertThat(result.longitude()).isEqualTo(128.913725);
        assertThat(result.imageUrl()).isEqualTo("https://example.com/store.jpg");
    }
}
