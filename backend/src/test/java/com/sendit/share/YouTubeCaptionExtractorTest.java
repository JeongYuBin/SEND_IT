package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class YouTubeCaptionExtractorTest {
    @Test
    void removesVttTimingMarkupAndDuplicateCaptionLines() {
        var extractor = new YouTubeCaptionExtractor(".", "yt-dlp", 10);
        String result = extractor.clean("""
                WEBVTT
                Kind: captions
                00:00:01.000 --> 00:00:02.000
                어 <00:00:01.200><c>하이탕 마라탕</c>
                어 하이탕 마라탕
                [음악]
                """);

        assertThat(result).isEqualTo("어 하이탕 마라탕");
    }
}
