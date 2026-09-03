package com.sendit.share;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MediaStorageCleaner {
    private static final Pattern SAFE_KEY = Pattern.compile(
            "[0-9a-f-]+(?:-frame-[0-9]{2}|-audio)?\\.(?:mp4|webm|mov|mkv|jpg|wav)");
    private final Path storageRoot;
    private final Duration transientRetention;

    @Autowired
    public MediaStorageCleaner(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.media.transient-retention-hours:24}") long retentionHours) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.transientRetention = Duration.ofHours(Math.max(1, retentionHours));
    }

    MediaStorageCleaner(String storageDirectory) {
        this(storageDirectory, 24);
    }

    public void deleteAll(Collection<String> storageKeys) {
        if (storageKeys == null) return;
        storageKeys.stream().filter(key -> key != null && !key.isBlank()).forEach(this::delete);
    }

    public void deleteTransient(String videoKey, String audioKey) {
        delete(videoKey);
        delete(audioKey);
    }

    @Scheduled(fixedDelayString = "${app.media.cleanup-delay-ms:3600000}")
    public void deleteExpiredTransientFiles() {
        if (!Files.isDirectory(storageRoot)) return;
        Instant cutoff = Instant.now().minus(transientRetention);
        try (var files = Files.list(storageRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> isTransient(path.getFileName().toString()))
                    .filter(path -> lastModifiedBefore(path, cutoff))
                    .forEach(path -> delete(path.getFileName().toString()));
        } catch (IOException ignored) {
            // 다음 정리 주기에 다시 시도한다.
        }
    }

    private boolean isTransient(String key) {
        return key.matches("[0-9a-f-]+\\.(?:mp4|webm|mov|mkv)")
                || key.matches("[0-9a-f-]+-audio\\.wav")
                || key.matches("stt-[0-9a-f-]+\\.txt");
    }

    private boolean lastModifiedBefore(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException ignored) {
            return false;
        }
    }

    private void delete(String key) {
        if (key == null || key.isBlank()) return;
        if (!SAFE_KEY.matcher(key).matches()) return;
        Path target = storageRoot.resolve(key).normalize();
        if (!target.startsWith(storageRoot)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // DB 콘텐츠 삭제는 유지하고 실패한 파일은 운영 정리 작업에서 재처리한다.
        }
    }
}
