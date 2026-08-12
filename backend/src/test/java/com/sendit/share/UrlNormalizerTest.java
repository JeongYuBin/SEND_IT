package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlNormalizerTest {
    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void removesYoutubeShareTokenFromShortAndShortsUrls() {
        assertThat(normalizer.normalize(
                "https://youtu.be/iOB0R7YMQ4o?si=qC_XrDVkIhl727It"))
                .isEqualTo("https://youtu.be/iOB0R7YMQ4o");
        assertThat(normalizer.normalize(
                "https://youtube.com/shorts/zXEI9WrtWfc?si=rpOYeIYq-SKg6HNm"))
                .isEqualTo("https://youtube.com/shorts/zXEI9WrtWfc");
    }

    @Test
    void keepsSiParameterForUnrelatedWebsites() {
        assertThat(normalizer.normalize("https://example.com/place?si=meaningful"))
                .isEqualTo("https://example.com/place?si=meaningful");
    }
}
