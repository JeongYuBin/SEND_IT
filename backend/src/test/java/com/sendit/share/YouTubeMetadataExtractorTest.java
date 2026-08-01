package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class YouTubeMetadataExtractorTest {
    private final YouTubeMetadataExtractor extractor =
            new YouTubeMetadataExtractor(new ObjectMapper(), "", 1, 1);

    @Test
    void extractsVideoIdsFromSupportedUrls() {
        assertThat(extractor.videoId("https://www.youtube.com/watch?v=abcDEF_1234"))
                .isEqualTo("abcDEF_1234");
        assertThat(extractor.videoId("https://youtu.be/abcDEF_1234?t=10"))
                .isEqualTo("abcDEF_1234");
        assertThat(extractor.videoId("https://www.youtube.com/shorts/abcDEF_1234"))
                .isEqualTo("abcDEF_1234");
        assertThat(extractor.videoId("https://www.youtube.com/embed/abcDEF_1234"))
                .isEqualTo("abcDEF_1234");
    }

    @Test
    void parsesOfficialApiSnippet() {
        String response = """
                {"items":[{"snippet":{
                  "title":"속초 여행",
                  "description":"속초해수욕장 방문기",
                  "thumbnails":{"high":{"url":"https://img.youtube.com/high.jpg"}}
                }}]}
                """;

        PageMetadata metadata = extractor.parse(response).orElseThrow();

        assertThat(metadata.title()).isEqualTo("속초 여행");
        assertThat(metadata.description()).isEqualTo("속초해수욕장 방문기");
        assertThat(metadata.imageUrl()).isEqualTo("https://img.youtube.com/high.jpg");
    }
}
