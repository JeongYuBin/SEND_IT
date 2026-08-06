package com.sendit.share;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VideoMediaProcessor {
    private static final Pattern SAFE_KEY = Pattern.compile("[0-9a-f-]+\\.(mp4|webm|mov|mkv)");
    private final Path storageRoot;
    private final String ffmpeg;
    private final String ffprobe;
    private final Duration timeout;

    public VideoMediaProcessor(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.media.ffmpeg-executable}") String ffmpeg,
            @Value("${app.media.ffprobe-executable}") String ffprobe,
            @Value("${app.media.processing-timeout-seconds}") long timeoutSeconds
    ) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public MediaProcessingResult process(String storageKey) {
        if (storageKey == null || !SAFE_KEY.matcher(storageKey).matches()) {
            throw new IllegalArgumentException("올바르지 않은 영상 저장 키입니다.");
        }
        Path input = storageRoot.resolve(storageKey).normalize();
        if (!input.startsWith(storageRoot) || !Files.isRegularFile(input)) {
            throw new ContentAnalysisException("분석할 영상 파일을 찾지 못했습니다.");
        }
        String base = storageKey.substring(0, storageKey.lastIndexOf('.'));
        double duration = probeDuration(input);
        double interval = Math.max(1.0, duration / 12.0);
        Path frameTemplate = storageRoot.resolve(base + "-frame-%02d.jpg");
        runRequired(List.of(
                ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
                "-i", input.toString(),
                "-vf", String.format(Locale.ROOT,
                        "fps=1/%.3f,scale=1280:-2:force_original_aspect_ratio=decrease", interval),
                "-frames:v", "12", frameTemplate.toString()
        ), "대표 프레임을 추출하지 못했습니다.");
        List<String> frames = findFrames(base);
        if (frames.isEmpty()) {
            throw new ContentAnalysisException("영상에서 대표 프레임을 찾지 못했습니다.");
        }
        String audioKey = base + "-audio.wav";
        Path audio = storageRoot.resolve(audioKey);
        boolean audioReady = runOptional(List.of(
                ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
                "-i", input.toString(), "-vn", "-ac", "1", "-ar", "16000",
                "-c:a", "pcm_s16le", audio.toString()
        ));
        if (!audioReady) {
            try { Files.deleteIfExists(audio); } catch (IOException ignored) { }
            audioKey = null;
        }
        return new MediaProcessingResult(duration, frames, audioKey);
    }

    private double probeDuration(Path input) {
        ProcessResult result = run(List.of(
                ffprobe, "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", input.toString()
        ));
        if (result.exitCode() != 0) {
            throw new ContentAnalysisException("영상 길이를 확인하지 못했습니다.");
        }
        try {
            double duration = Double.parseDouble(result.output().trim());
            if (!Double.isFinite(duration) || duration <= 0) throw new NumberFormatException();
            return duration;
        } catch (NumberFormatException exception) {
            throw new ContentAnalysisException("영상 길이 정보가 올바르지 않습니다.");
        }
    }

    private List<String> findFrames(String base) {
        try (var files = Files.list(storageRoot)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith(base + "-frame-") && name.endsWith(".jpg"))
                    .sorted(Comparator.naturalOrder())
                    .limit(12)
                    .toList();
        } catch (IOException exception) {
            throw new ContentAnalysisException("대표 프레임 목록을 읽지 못했습니다.");
        }
    }

    private void runRequired(List<String> command, String message) {
        if (run(command).exitCode() != 0) throw new ContentAnalysisException(message);
    }

    private boolean runOptional(List<String> command) {
        return run(command).exitCode() == 0;
    }

    private ProcessResult run(List<String> command) {
        try {
            Process process = new ProcessBuilder(new ArrayList<>(command))
                    .redirectErrorStream(true).start();
            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ContentAnalysisException("영상 처리 시간이 초과되었습니다.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ProcessResult(process.exitValue(), output);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ContentAnalysisException("영상 처리 작업이 중단되었습니다.");
        } catch (IOException exception) {
            throw new ContentAnalysisException("영상 처리 도구를 실행하지 못했습니다.");
        }
    }

    private record ProcessResult(int exitCode, String output) { }
}
