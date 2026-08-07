package com.sendit.share;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MediaStorageCleaner {
    private static final Pattern SAFE_KEY = Pattern.compile(
            "[0-9a-f-]+(?:-frame-[0-9]{2}|-audio)?\\.(?:mp4|webm|mov|mkv|jpg|wav)");
    private final Path storageRoot;

    public MediaStorageCleaner(@Value("${app.media.storage-directory}") String storageDirectory) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public void deleteAll(Collection<String> storageKeys) {
        if (storageKeys == null) return;
        storageKeys.stream().filter(key -> key != null && !key.isBlank()).forEach(this::delete);
    }

    private void delete(String key) {
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
