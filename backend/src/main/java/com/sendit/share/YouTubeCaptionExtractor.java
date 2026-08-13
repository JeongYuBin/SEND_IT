package com.sendit.share;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class YouTubeCaptionExtractor {
    private static final Pattern TIMESTAMP = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*$");
    private final Path storageRoot;
    private final String executable;
    private final Duration timeout;

    public YouTubeCaptionExtractor(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.media.downloader-executable}") String executable,
            @Value("${app.media.download-timeout-seconds}") long timeoutSeconds) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.executable = executable;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public String extract(String url) {
        if (!isYouTube(url)) return null;
        String prefix = "caption-" + UUID.randomUUID();
        Path template = storageRoot.resolve(prefix + ".%(ext)s");
        try {
            Files.createDirectories(storageRoot);
            Process process = new ProcessBuilder(List.of(
                    executable, "--no-config-locations", "--no-playlist", "--skip-download",
                    "--write-subs", "--write-auto-subs", "--sub-langs", "ko",
                    "--sub-format", "vtt", "--output", template.toString(), url))
                    .redirectErrorStream(true).start();
            if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            Path caption = find(prefix);
            return caption == null ? null : clean(Files.readString(caption, StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException ignored) {
            return null;
        } finally {
            cleanup(prefix);
        }
    }

    String clean(String vtt) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (String raw : vtt.lines().toList()) {
            String line = raw.replaceAll("<[^>]+>", "")
                    .replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
            if (line.isBlank() || line.equals("WEBVTT") || line.startsWith("Kind:")
                    || line.startsWith("Language:") || TIMESTAMP.matcher(line).matches()
                    || line.matches("^\\[.+]$")) continue;
            lines.add(line);
        }
        String text = String.join("\n", lines);
        return text.length() <= 50_000 ? text : text.substring(0, 50_000);
    }

    private boolean isYouTube(String url) {
        return url != null && url.matches("https://(?:www\\.|m\\.)?(?:youtube\\.com|youtu\\.be)/.*");
    }

    private Path find(String prefix) throws IOException {
        try (var files = Files.list(storageRoot)) {
            return files.filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(".vtt"))
                    .findFirst().orElse(null);
        }
    }

    private void cleanup(String prefix) {
        try (var files = Files.exists(storageRoot) ? Files.list(storageRoot) : null) {
            if (files != null) files.filter(path -> path.getFileName().toString().startsWith(prefix))
                    .forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }
}
