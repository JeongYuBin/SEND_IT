package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageMetadataParserTest {

    private final PageMetadataParser parser = new PageMetadataParser();

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
}

