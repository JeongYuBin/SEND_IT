package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleShareMetadataExtractorTest {

    private final GoogleShareMetadataExtractor extractor = new GoogleShareMetadataExtractor(null);

    @Test
    void supportsOnlyGoogleShareLinks() {
        assertThat(extractor.supports("https://share.google/UpZeA7GfFOQK7HzVj")).isTrue();
        assertThat(extractor.supports("https://share.google.evil.example/value")).isFalse();
    }

    @Test
    void extractsPlaceQueryFromGoogleFallbackLink() {
        String html = """
                <html><body>
                  <a href="/search?q=%EB%8D%94%EA%B8%B0%EC%99%80+%ED%95%A9%EC%A0%95%EC%A0%90&amp;sca_esv=123">여기</a>
                </body></html>
                """;

        PageMetadata metadata = extractor.parse(html, "https://www.google.com/share.google?q=token")
                .orElseThrow();

        assertThat(metadata.title()).isEqualTo("더기와 합정점");
        assertThat(metadata.placeName()).isEqualTo("더기와 합정점");
    }
}
