package com.sendit.share;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InstagramCarouselDownloader {
    private static final Logger log = LoggerFactory.getLogger(InstagramCarouselDownloader.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36";

    private final Path storageRoot;
    private final HttpClient http;
    private final Duration timeout;
    private final long maxBytes;

    public InstagramCarouselDownloader(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds,
            @Value("${app.media.max-bytes}") long maxBytes
    ) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds)).build();
        this.timeout = Duration.ofSeconds(requestTimeoutSeconds);
        this.maxBytes = maxBytes;
    }

    public boolean supports(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() != null && uri.getHost().toLowerCase().endsWith("instagram.com")
                    && uri.getPath() != null && (uri.getPath().startsWith("/p/")
                    || uri.getPath().startsWith("/reel/"));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public MediaProcessingResult downloadFrames(String url) {
        if (!supports(url)) return new MediaProcessingResult(0, List.of(), null);
        try {
            String embedUrl = canonical(url) + "embed/captioned/";
            HttpRequest pageRequest = HttpRequest.newBuilder(URI.create(embedUrl))
                    .timeout(timeout).header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.7").GET().build();
            HttpResponse<String> page = http.send(pageRequest,
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (page.statusCode() < 200 || page.statusCode() >= 300) return empty();
            List<String> urls = imageUrls(page.body());
            if (urls.size() < 2) {
                log.warn("Instagram carousel images not found: status={}, htmlLength={}, url={}",
                        page.statusCode(), page.body().length(), canonical(url));
                log.warn("Instagram embed markers: sidecar={}, displayUrl={}, https={}",
                        page.body().indexOf("edge_sidecar_to_children"),
                        page.body().indexOf("display_url"), page.body().indexOf("https"));
                return empty();
            }

            Files.createDirectories(storageRoot);
            String prefix = UUID.randomUUID().toString();
            List<String> keys = new ArrayList<>();
            long totalBytes = 0;
            for (String imageUrl : urls.stream().limit(20).toList()) {
                HttpRequest imageRequest = HttpRequest.newBuilder(URI.create(imageUrl))
                        .timeout(timeout).header("User-Agent", USER_AGENT)
                        .header("Referer", embedUrl).GET().build();
                HttpResponse<byte[]> image = http.send(imageRequest,
                        HttpResponse.BodyHandlers.ofByteArray());
                if (image.statusCode() < 200 || image.statusCode() >= 300
                        || image.body().length == 0) continue;
                totalBytes += image.body().length;
                if (totalBytes > maxBytes) break;
                String key = prefix + "-frame-" + String.format("%02d", keys.size() + 1) + ".jpg";
                Files.write(storageRoot.resolve(key), image.body());
                keys.add(key);
            }
            log.info("Downloaded {} Instagram carousel images for {}", keys.size(), canonical(url));
            return new MediaProcessingResult(0, keys, null);
        } catch (Exception exception) {
            log.warn("Instagram carousel download failed for {}: {}", canonical(url),
                    exception.getMessage());
            return empty();
        }
    }

    List<String> imageUrls(String html) {
        if (!html.contains("edge_sidecar_to_children")) return List.of();
        String section = html;
        Set<String> urls = new LinkedHashSet<>();
        int cursor = 0;
        while (cursor < section.length()) {
            int field = section.indexOf("display_url", cursor);
            if (field < 0) break;
            int start = section.indexOf("https", field);
            int boundary = start < 0 ? -1 : section.indexOf("display_resources", start);
            if (start < 0 || boundary < 0) break;
            String raw = section.substring(start, boundary)
                    .replaceFirst("[\\\\\\\"',:{}\\s]+$", "");
            String decoded = decode(raw);
            if (decoded.startsWith("https://")) urls.add(decoded);
            cursor = boundary + "display_resources".length();
        }
        return List.copyOf(urls);
    }

    private String canonical(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath().endsWith("/") ? uri.getPath() : uri.getPath() + "/";
        return "https://www.instagram.com" + path;
    }

    private String decode(String value) {
        String decoded = value;
        while (decoded.contains("\\u00253D")) decoded = decoded.replace("\\u00253D", "%3D");
        while (decoded.contains("\\u0026")) decoded = decoded.replace("\\u0026", "&");
        return decoded.replace("\\", "");
    }

    private MediaProcessingResult empty() {
        return new MediaProcessingResult(0, List.of(), null);
    }
}
