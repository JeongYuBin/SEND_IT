package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TikTokMetadataExtractorTest {
    private final TikTokMetadataExtractor extractor =
            new TikTokMetadataExtractor(new ObjectMapper(), 1, 1);

    @Test
    void supportsTikTokVideoAndShortLinkHosts() {
        assertThat(extractor.supports(
                "https://www.tiktok.com/@creator/video/6718335390845095173")).isTrue();
        assertThat(extractor.supports("https://vm.tiktok.com/example/")).isTrue();
        assertThat(extractor.supports("https://tiktok.com.evil.example/video/1")).isFalse();
    }

    @Test
    void parsesOfficialOEmbedMetadata() {
        PageMetadata metadata = extractor.parse("""
                {
                  "title":"속초 카페 방문 #속초카페",
                  "author_name":"여행자",
                  "thumbnail_url":"https://example.com/tiktok.jpg"
                }
                """).orElseThrow();

        assertThat(metadata.title()).isEqualTo("속초 카페 방문 #속초카페");
        assertThat(metadata.description()).contains("TikTok 게시자: 여행자");
        assertThat(metadata.imageUrl()).isEqualTo("https://example.com/tiktok.jpg");
    }
}
