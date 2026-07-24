package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void removesTrackingParametersFragmentAndTrailingSlash() {
        String result = normalizer.normalize(
                "https://Blog.Naver.com/travel/?b=2&utm_source=sns&a=1#section");

        assertThat(result).isEqualTo("https://blog.naver.com/travel?a=1&b=2");
    }

    @Test
    void rejectsNonHttpUrl() {
        assertThatThrownBy(() -> normalizer.normalize("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detectsKnownSourceFromHost() {
        assertThat(normalizer.detectSource("https://youtu.be/example"))
                .isEqualTo(SourceType.YOUTUBE);
    }
}

