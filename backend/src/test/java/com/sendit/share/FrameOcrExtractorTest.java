package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrameOcrExtractorTest {
    @TempDir
    Path tempDir;

    @Test
    void rejectsUnsafeFrameStorageKey() {
        var extractor = new FrameOcrExtractor(tempDir.toString(), "tesseract", 5, 1000);

        assertThatThrownBy(() -> extractor.extract(List.of("../secret.jpg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("저장 키");
    }
}
