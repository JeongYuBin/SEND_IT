package com.sendit.share;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "video/mp4", ".mp4",
            "video/quicktime", ".mov",
            "video/webm", ".webm"
    );

    private final Path storageRoot;
    private final long maxBytes;

    public MediaStorageService(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.media.max-bytes}") long maxBytes
    ) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public StoredMedia store(MultipartFile file) {
        validate(file);
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String storageKey = UUID.randomUUID() + EXTENSIONS.get(contentType);
        Path target = storageRoot.resolve(storageKey).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("올바르지 않은 영상 저장 경로입니다.");
        }
        try {
            Files.createDirectories(storageRoot);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredMedia(
                    storageKey,
                    safeFilename(file.getOriginalFilename()),
                    contentType,
                    file.getSize()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("영상 파일을 저장하지 못했습니다.", exception);
        }
    }

    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        Path target = storageRoot.resolve(storageKey).normalize();
        if (!target.startsWith(storageRoot)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // DB 저장 실패 시 보조 정리이므로 원래 예외를 유지한다.
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("분석할 영상 파일을 선택해 주세요.");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("영상 파일은 100MB 이하만 업로드할 수 있습니다.");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.containsKey(contentType)) {
            throw new IllegalArgumentException("MP4, MOV, WebM 영상만 업로드할 수 있습니다.");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (!matchesSignature(contentType, header)) {
                throw new IllegalArgumentException("영상 파일 형식을 확인해 주세요.");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("영상 파일을 읽지 못했습니다.", exception);
        }
    }

    private boolean matchesSignature(String contentType, byte[] header) {
        if ("video/webm".equals(contentType)) {
            return header.length >= 4
                    && header[0] == 0x1A && header[1] == 0x45
                    && (header[2] & 0xFF) == 0xDF && (header[3] & 0xFF) == 0xA3;
        }
        return header.length >= 8
                && header[4] == 'f' && header[5] == 't'
                && header[6] == 'y' && header[7] == 'p';
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "video";
        String normalized = filename.replace('\\', '/');
        String safe = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\r\\n]", "");
        if (safe.isBlank()) safe = "video";
        return safe.length() <= 500 ? safe : safe.substring(safe.length() - 500);
    }
}
