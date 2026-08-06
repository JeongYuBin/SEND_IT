package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class MediaStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesMp4WithGeneratedSafeKey() throws Exception {
        byte[] content = new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        var file = new MockMultipartFile(
                "file", "../여행 영상.mp4", "video/mp4", content);
        var service = new MediaStorageService(tempDir.toString(), 1024);

        StoredMedia stored = service.store(file);

        assertThat(stored.storageKey()).endsWith(".mp4").doesNotContain("..");
        assertThat(stored.originalFilename()).isEqualTo("여행 영상.mp4");
        assertThat(Files.readAllBytes(tempDir.resolve(stored.storageKey()))).isEqualTo(content);
    }

    @Test
    void rejectsFileWhoseSignatureDoesNotMatchVideoType() {
        var file = new MockMultipartFile(
                "file", "fake.mp4", "video/mp4", "not-a-video".getBytes());
        var service = new MediaStorageService(tempDir.toString(), 1024);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("영상 파일 형식을 확인해 주세요.");
    }

    @Test
    void rejectsOversizedVideo() {
        byte[] content = new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        var file = new MockMultipartFile("file", "large.mp4", "video/mp4", content);
        var service = new MediaStorageService(tempDir.toString(), 8);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100MB 이하");
    }
}
