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
public class YouTubeMetadataExtractor implements PlatformMetadataExtractor {
    private static final String API_URL = "https://www.googleapis.com/youtube/v3/videos";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final Duration requestTimeout;

    public YouTubeMetadataExtractor(
            ObjectMapper objectMapper,
            @Value("${app.youtube.api-key:}") String apiKey,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
    }

    @Override
    public boolean supports(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && (host.equalsIgnoreCase("youtu.be")
                    || host.toLowerCase().endsWith("youtube.com"));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public Optional<PageMetadata> extract(String url) {
        String videoId = videoId(url);
        if (apiKey.isBlank() || videoId == null) return Optional.empty();
        try {
            URI uri = URI.create(API_URL + "?part=snippet&id=" + encode(videoId)
                    + "&key=" + encode(apiKey));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            return parse(response.body());
        } catch (Exception ignored) {
            // API 키, 할당량 또는 네트워크 문제 시 기존 HTML 메타데이터 분석으로 대체한다.
            return Optional.empty();
        }
    }

    String videoId(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (host.equals("youtu.be")) return validId(firstPathPart(path));
            if (!host.endsWith("youtube.com")) return null;
            if (path.equals("/watch")) return validId(queryParameter(uri.getRawQuery(), "v"));
            if (path.startsWith("/shorts/") || path.startsWith("/embed/")) {
                return validId(firstPathPart(path.substring(path.indexOf('/', 1))));
            }
            return null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    Optional<PageMetadata> parse(String body) {
        try {
            JsonNode items = objectMapper.readTree(body).path("items");
            if (!items.isArray() || items.isEmpty()) return Optional.empty();
            JsonNode snippet = items.get(0).path("snippet");
            String title = text(snippet, "title");
            String description = text(snippet, "description");
            String image = thumbnail(snippet.path("thumbnails"));
            if (title == null && description == null && image == null) return Optional.empty();
            return Optional.of(new PageMetadata(
                    title, description, image, null, null, null, null, null));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String thumbnail(JsonNode thumbnails) {
        for (String size : new String[]{"maxres", "standard", "high", "medium", "default"}) {
            String url = text(thumbnails.path(size), "url");
            if (url != null) return url;
        }
        return null;
    }

    private String queryParameter(String query, String name) {
        if (query == null) return null;
        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            if (parts[0].equals(name) && parts.length == 2) return parts[1];
        }
        return null;
    }

    private String firstPathPart(String path) {
        String value = path.replaceFirst("^/+", "").split("/", 2)[0];
        return value.isBlank() ? null : value;
    }

    private String validId(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{6,20}") ? value : null;
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
