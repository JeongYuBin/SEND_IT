package com.sendit.share;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FrameOcrExtractor {
    private static final Pattern SAFE_FRAME_KEY =
            Pattern.compile("[0-9a-f-]+-frame-[0-9]{2}\\.jpg");
    private final Path storageRoot;
    private final String executable;
    private final Duration timeoutPerFrame;
    private final int maxTextLength;

    public FrameOcrExtractor(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.media.tesseract-executable}") String executable,
            @Value("${app.media.ocr-timeout-seconds}") long timeoutSeconds,
            @Value("${app.media.ocr-max-text-length}") int maxTextLength
    ) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.executable = executable;
        this.timeoutPerFrame = Duration.ofSeconds(timeoutSeconds);
        this.maxTextLength = maxTextLength;
    }

    public String extract(List<String> frameStorageKeys) {
        if (frameStorageKeys == null || frameStorageKeys.isEmpty()) return null;
        Set<String> lines = new LinkedHashSet<>();
        frameStorageKeys.stream().limit(12).forEach(key -> collectFrameText(key, lines));
        String combined = String.join("\n", lines);
        if (combined.isBlank()) return null;
        return combined.length() <= maxTextLength
                ? combined : combined.substring(0, maxTextLength);
    }

    private void collectFrameText(String key, Set<String> lines) {
        if (key == null || !SAFE_FRAME_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("올바르지 않은 대표 프레임 저장 키입니다.");
        }
        Path frame = storageRoot.resolve(key).normalize();
        if (!frame.startsWith(storageRoot) || !Files.isRegularFile(frame)) {
            throw new ContentAnalysisException("OCR 대상 대표 프레임을 찾지 못했습니다.");
        }
        Process process = null;
        try {
            process = new ProcessBuilder(
                    executable, frame.toString(), "stdout",
                    "-l", "kor+eng", "--psm", "11")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            boolean finished = process.waitFor(timeoutPerFrame.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return;
            }
            if (process.exitValue() != 0) return;
            String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            text.lines().map(String::trim)
                    .filter(line -> line.length() >= 2)
                    .filter(line -> line.chars().anyMatch(Character::isLetterOrDigit))
                    .forEach(lines::add);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ContentAnalysisException("프레임 OCR 작업이 중단되었습니다.");
        } catch (IOException exception) {
            throw new ContentAnalysisException("OCR 도구를 실행하지 못했습니다.");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }
}
