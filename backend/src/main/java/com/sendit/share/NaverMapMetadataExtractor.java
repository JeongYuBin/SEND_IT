package com.sendit.share;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NaverMapMetadataExtractor implements PlatformMetadataExtractor {
    private static final String SUMMARY_URL = "https://map.naver.com/p/api/place/summary/";
    private static final Pattern PLACE_PATH = Pattern.compile("/place/(\\d{5,20})(?:/|$)");
    private static final Pattern PLACE_QUERY = Pattern.compile("(?:^|&)(?:pinId|id)=(\\d{5,20})(?:&|$)");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    @Autowired
    public NaverMapMetadataExtractor(
            ObjectMapper objectMapper,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), Duration.ofSeconds(requestTimeoutSeconds));
    }

    NaverMapMetadataExtractor(
            ObjectMapper objectMapper, HttpClient httpClient, Duration requestTimeout) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public boolean supports(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return false;
            String normalized = host.toLowerCase();
            return normalized.equals("naver.me")
                    || normalized.equals("map.naver.com")
                    || normalized.equals("m.map.naver.com");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public Optional<PageMetadata> extract(String url) {
        if (!supports(url)) return Optional.empty();
        try {
            String placeId = placeId(URI.create(url));
            if (placeId == null) {
                HttpRequest redirectRequest = HttpRequest.newBuilder(URI.create(url))
                        .timeout(requestTimeout)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) SEND-IT/1.0")
                        .GET()
                        .build();
                HttpResponse<Void> redirectResponse = httpClient.send(
                        redirectRequest, HttpResponse.BodyHandlers.discarding());
                placeId = placeId(redirectResponse.uri());
            }
            if (placeId == null) return Optional.empty();

            HttpRequest detailRequest = HttpRequest.newBuilder(URI.create(SUMMARY_URL + placeId))
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .header("Referer", "https://map.naver.com/")
                    .header("User-Agent", "SEND-IT/1.0")
                    .GET()
                    .build();
            HttpResponse<String> detailResponse = httpClient.send(
                    detailRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (detailResponse.statusCode() < 200 || detailResponse.statusCode() >= 300) {
                return Optional.empty();
            }
            return parse(detailResponse.body());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    String placeId(URI uri) {
        Matcher pathMatcher = PLACE_PATH.matcher(uri.getPath() == null ? "" : uri.getPath());
        if (pathMatcher.find()) return pathMatcher.group(1);
        Matcher queryMatcher = PLACE_QUERY.matcher(uri.getRawQuery() == null ? "" : uri.getRawQuery());
        return queryMatcher.find() ? queryMatcher.group(1) : null;
    }

    Optional<PageMetadata> parse(String body) {
        try {
            JsonNode place = objectMapper.readTree(body).path("data").path("placeDetail");
            String name = text(place, "name");
            if (name == null) return Optional.empty();
            JsonNode address = place.path("address");
            JsonNode coordinate = place.path("coordinate");
            String category = first(text(place.path("category"), "category"),
                    text(place, "businessType"));
            String roadAddress = text(address, "roadAddress");
            String lotAddress = text(address, "address");
            String image = firstImage(place.path("images").path("images"));
            return Optional.of(new PageMetadata(
                    name, null, image, name, category,
                    first(roadAddress, lotAddress),
                    number(coordinate, "latitude"), number(coordinate, "longitude")));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String firstImage(JsonNode images) {
        if (!images.isArray()) return null;
        for (JsonNode image : images) {
            String origin = text(image, "origin");
            if (origin != null) return origin;
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Double number(JsonNode node, String field) {
        return node.path(field).isNumber() ? node.path(field).doubleValue() : null;
    }

    private String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
