package com.sendit.share;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AudioTranscriber {
    private static final Pattern SAFE_AUDIO_KEY =
            Pattern.compile("[0-9a-f-]+-audio\\.wav");
    private final Path storageRoot;
    private final String executable;
    private final Path model;
    private final Duration timeout;
    private final int maxTextLength;

    public AudioTranscriber(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.media.whisper-executable}") String executable,
            @Value("${app.media.whisper-model}") String model,
            @Value("${app.media.transcription-timeout-seconds}") long timeoutSeconds,
            @Value("${app.media.transcription-max-text-length}") int maxTextLength
    ) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.executable = executable;
        this.model = Path.of(model).toAbsolutePath().normalize();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.maxTextLength = maxTextLength;
    }

    public String transcribe(String audioStorageKey) {
        if (audioStorageKey == null || !SAFE_AUDIO_KEY.matcher(audioStorageKey).matches()) {
            throw new IllegalArgumentException("올바르지 않은 음원 파일 키입니다.");
        }
        Path audio = storageRoot.resolve(audioStorageKey).normalize();
        if (!audio.startsWith(storageRoot) || !Files.isRegularFile(audio)) {
            throw new ContentAnalysisException("STT 대상 음원 파일을 찾지 못했습니다.");
        }
        if (!Files.isRegularFile(model)) {
            throw new ContentAnalysisException("STT 모델 파일을 찾지 못했습니다.");
        }

        Path outputBase = storageRoot.resolve("stt-" + UUID.randomUUID()).normalize();
        Path outputText = Path.of(outputBase + ".txt");
        Process process = null;
        try {
            process = new ProcessBuilder(
                    executable, "-m", model.toString(), "-f", audio.toString(),
                    "-l", "auto", "-otxt", "-of", outputBase.toString(),
                    "-np", "-nt")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ContentAnalysisException("음성 인식 제한 시간을 초과했습니다.");
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(outputText)) {
                throw new ContentAnalysisException("음성을 텍스트로 변환하지 못했습니다.");
            }
            String transcript = Files.readString(outputText, StandardCharsets.UTF_8).trim();
            if (transcript.isBlank()) return null;
            return transcript.length() <= maxTextLength
                    ? transcript : transcript.substring(0, maxTextLength);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ContentAnalysisException("음성 인식 작업이 중단되었습니다.");
        } catch (IOException exception) {
            throw new ContentAnalysisException("음성 인식 도구를 실행하지 못했습니다.");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            try {
                Files.deleteIfExists(outputText);
            } catch (IOException ignored) {
                // 임시 자막 파일 정리 실패는 분석 결과에 영향을 주지 않는다.
            }
        }
    }
}
