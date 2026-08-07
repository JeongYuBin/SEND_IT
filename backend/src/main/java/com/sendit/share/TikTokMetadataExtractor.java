package com.sendit.share;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TikTokMetadataExtractor implements PlatformMetadataExtractor {
    private static final String OEMBED_URL = "https://www.tiktok.com/oembed";
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public TikTokMetadataExtractor(
            ObjectMapper objectMapper,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
    }

    @Override
    public boolean supports(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return false;
            String normalized = host.toLowerCase();
            return normalized.equals("tiktok.com") || normalized.endsWith(".tiktok.com");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public Optional<PageMetadata> extract(String url) {
        if (!supports(url)) return Optional.empty();
        try {
            URI endpoint = URI.create(OEMBED_URL + "?url="
                    + URLEncoder.encode(url, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .header("User-Agent", "SEND-IT/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            return parse(response.body());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    Optional<PageMetadata> parse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String title = text(root, "title");
            String author = text(root, "author_name");
            String thumbnail = text(root, "thumbnail_url");
            if (title == null && author == null && thumbnail == null) return Optional.empty();
            String description = title;
            if (author != null) {
                description = description == null
                        ? "TikTok 게시자: " + author
                        : description + "\nTikTok 게시자: " + author;
            }
            return Optional.of(new PageMetadata(
                    title, description, thumbnail, null, null, null, null, null));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
