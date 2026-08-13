package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MediaStorageCleanerTest {
    @TempDir Path tempDir;

    @Test
    void deletesOnlyValidatedMediaKeys() throws Exception {
        String key = "123e4567-e89b-12d3-a456-426614174000-audio.wav";
        Path media = Files.writeString(tempDir.resolve(key), "audio");
        Path outside = Files.writeString(tempDir.resolve("keep.txt"), "keep");
        MediaStorageCleaner cleaner = new MediaStorageCleaner(tempDir.toString());

        cleaner.deleteAll(List.of(key, "../keep.txt", "keep.txt"));

        assertThat(media).doesNotExist();
        assertThat(outside).exists();
    }

    @Test
    void deletesExpiredVideoAndAudioButKeepsFrames() throws Exception {
        Path video = Files.writeString(tempDir.resolve(
                "123e4567-e89b-12d3-a456-426614174000.mp4"), "video");
        Path audio = Files.writeString(tempDir.resolve(
                "123e4567-e89b-12d3-a456-426614174000-audio.wav"), "audio");
        Path frame = Files.writeString(tempDir.resolve(
                "123e4567-e89b-12d3-a456-426614174000-frame-01.jpg"), "frame");
        FileTime expired = FileTime.from(Instant.now().minusSeconds(25 * 60 * 60));
        Files.setLastModifiedTime(video, expired);
        Files.setLastModifiedTime(audio, expired);
        Files.setLastModifiedTime(frame, expired);

        new MediaStorageCleaner(tempDir.toString()).deleteExpiredTransientFiles();

        assertThat(video).doesNotExist();
        assertThat(audio).doesNotExist();
        assertThat(frame).exists();
    }
}
