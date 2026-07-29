package com.sendit.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KakaoTransitClient {

    private static final String API_URL = "https://dapi.kakao.com/v2/routing/publictraffic";
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final String restApiKey;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public KakaoTransitClient(
            ObjectMapper objectMapper,
            @Value("${app.kakao.rest-api-key:}") String restApiKey,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.restApiKey = restApiKey == null ? "" : restApiKey.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
    }

    public Optional<TransitRoute> route(Location start, Location end) {
        if (restApiKey.isBlank()) return Optional.empty();
        String cacheKey = key(start, end);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.route();
        }
        try {
            URI uri = URI.create(API_URL
                    + "?start_x=" + start.longitude()
                    + "&start_y=" + start.latitude()
                    + "&end_x=" + end.longitude()
                    + "&end_y=" + end.latitude()
                    + "&s_name=" + encode(start.name())
                    + "&e_name=" + encode(end.name()));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(requestTimeout)
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Optional<TransitRoute> route = response.statusCode() == 200
                    ? parse(response.body())
                    : Optional.empty();
            cache.put(cacheKey, new CacheEntry(route, Instant.now().plus(CACHE_DURATION)));
            return route;
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    Optional<TransitRoute> parse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!"OK".equals(root.path("status").asText())) return Optional.empty();
        JsonNode route = root.path("routes").path(0);
        if (route.isMissingNode()) return Optional.empty();
        JsonNode properties = route.path("properties");
        List<TransitStep> steps = new ArrayList<>();
        List<PathPoint> path = new ArrayList<>();
        route.path("steps").forEach(step -> {
            JsonNode stepProperties = step.path("properties");
            List<String> stops = new ArrayList<>();
            stepProperties.path("stops").forEach(stop -> stops.add(stop.path("name").asText()));
            List<String> vehicles = new ArrayList<>();
            stepProperties.path("vehicles").forEach(vehicle ->
                    vehicles.add(vehicle.path("name").asText()));
            step.path("path").path("points").forEach(point -> {
                if (point.size() >= 2) {
                    path.add(new PathPoint(
                            point.get(1).asDouble(),
                            point.get(0).asDouble()));
                }
            });
            steps.add(new TransitStep(
                    stepProperties.path("type").asText(),
                    stepProperties.path("guidance").asText(),
                    ceilMinutes(stepProperties.path("time").asInt()),
                    stepProperties.path("distance").asInt(),
                    stops.isEmpty() ? null : stops.getFirst(),
                    stops.isEmpty() ? null : stops.getLast(),
                    vehicles
            ));
        });
        return Optional.of(new TransitRoute(
                properties.path("type").asText(),
                ceilMinutes(properties.path("totalTime").asInt()),
                properties.path("totalDistance").asInt(),
                properties.path("transfers").asInt(),
                properties.path("fare").path("value").asInt(),
                root.path("properties").path("landingURL").asText(null),
                steps,
                path
        ));
    }

    private int ceilMinutes(int seconds) {
        return Math.max(1, (int) Math.ceil(seconds / 60.0));
    }

    private String key(Location start, Location end) {
        return "%.5f,%.5f:%.5f,%.5f".formatted(
                start.latitude(), start.longitude(), end.latitude(), end.longitude());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record Location(String name, double latitude, double longitude) {}
    public record TransitRoute(
            String type,
            int totalMinutes,
            int totalDistanceMeters,
            int transfers,
            int fare,
            String landingUrl,
            List<TransitStep> steps,
            List<PathPoint> path
    ) {}
    public record TransitStep(
            String type,
            String guidance,
            int minutes,
            int distanceMeters,
            String startStop,
            String endStop,
            List<String> vehicles
    ) {}
    public record PathPoint(double latitude, double longitude) {}
    private record CacheEntry(Optional<TransitRoute> route, Instant expiresAt) {}
}
