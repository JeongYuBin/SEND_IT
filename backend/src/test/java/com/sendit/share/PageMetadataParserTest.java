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
}
