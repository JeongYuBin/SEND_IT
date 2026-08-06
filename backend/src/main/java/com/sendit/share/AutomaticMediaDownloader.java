package com.sendit.share;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AutomaticMediaDownloader {
    private static final Set<String> SUPPORTED_HOSTS = Set.of(
            "instagram.com", "www.instagram.com",
            "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"
    );

    private final Path storageRoot;
    private final String executable;
    private final long maxBytes;
    private final Duration timeout;

    public AutomaticMediaDownloader(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.media.downloader-executable}") String executable,
            @Value("${app.media.max-bytes}") long maxBytes,
            @Value("${app.media.download-timeout-seconds}") long timeoutSeconds
    ) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.executable = executable;
        this.maxBytes = maxBytes;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public boolean supports(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && SUPPORTED_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public StoredMedia download(String url) {
        if (!supports(url)) {
            throw new IllegalArgumentException("자동 영상 확보를 지원하지 않는 주소입니다.");
        }
        String keyPrefix = UUID.randomUUID().toString();
        Path outputTemplate = storageRoot.resolve(keyPrefix + ".%(ext)s").normalize();
        try {
            Files.createDirectories(storageRoot);
            Process process = new ProcessBuilder(command(url, outputTemplate))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                cleanup(keyPrefix);
                throw new ContentAnalysisException("영상 확보 시간이 초과되었습니다.");
            }
            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                cleanup(keyPrefix);
                throw new ContentAnalysisException("공개 영상을 확보하지 못했습니다: " + summarize(output));
            }
            Path downloaded = findDownloaded(keyPrefix);
            long size = Files.size(downloaded);
            if (size <= 0 || size > maxBytes) {
                cleanup(keyPrefix);
                throw new ContentAnalysisException("영상 파일 크기가 허용 범위를 벗어났습니다.");
            }
            String filename = downloaded.getFileName().toString();
            return new StoredMedia(filename, filename, contentType(filename), size);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cleanup(keyPrefix);
            throw new ContentAnalysisException("영상 확보 작업이 중단되었습니다.");
        } catch (IOException exception) {
            cleanup(keyPrefix);
            throw new ContentAnalysisException("영상 확보 도구를 실행하지 못했습니다.");
        }
    }

    private List<String> command(String url, Path outputTemplate) {
        return List.of(
                executable,
                "--no-config-locations",
                "--no-playlist",
                "--no-progress",
                "--no-warnings",
                "--js-runtimes", "node:/usr/bin/node",
                "--max-filesize", String.valueOf(maxBytes),
                "--restrict-filenames",
                "--format", "bv*+ba/b",
                "--merge-output-format", "mp4",
                "--output", outputTemplate.toString(),
                "--print", "after_move:filepath",
                url
        );
    }

    private Path findDownloaded(String keyPrefix) throws IOException {
        try (var paths = Files.list(storageRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(keyPrefix + "."))
                    .findFirst()
                    .orElseThrow(() -> new ContentAnalysisException("확보된 영상 파일을 찾지 못했습니다."));
        }
    }

    private void cleanup(String keyPrefix) {
        try (var paths = Files.exists(storageRoot) ? Files.list(storageRoot) : null) {
            if (paths == null) return;
            paths.filter(path -> path.getFileName().toString().startsWith(keyPrefix + "."))
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                    });
        } catch (IOException ignored) { }
    }

    private String contentType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mov")) return "video/quicktime";
        return "video/mp4";
    }

    private String summarize(String output) {
        if (output == null || output.isBlank()) return "접근할 수 없는 게시물입니다.";
        String singleLine = output.replaceAll("[\\r\\n]+", " ").trim();
        return singleLine.length() <= 300 ? singleLine : singleLine.substring(0, 300);
    }
}
