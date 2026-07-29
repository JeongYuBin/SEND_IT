package com.sendit.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendit.itinerary.TransportType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KakaoDirectionsClient {

    private static final String CAR_API_URL =
            "https://apis-navi.kakaomobility.com/v1/directions";
    private static final String WALK_API_URL =
            "https://dapi.kakao.com/v2/routing/walk";
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final String restApiKey;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public KakaoDirectionsClient(
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

    public Optional<RouteEstimate> route(
            TransportType transportType,
            Location start,
            Location end
    ) {
        if (restApiKey.isBlank()
                || (transportType != TransportType.CAR
                && transportType != TransportType.WALKING)) {
            return Optional.empty();
        }
        String cacheKey = "%s:%.5f,%.5f:%.5f,%.5f".formatted(
                transportType,
                start.latitude(), start.longitude(),
                end.latitude(), end.longitude());
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.route();
        }
        Optional<RouteEstimate> route = request(transportType, start, end);
        cache.put(cacheKey, new CacheEntry(route, Instant.now().plus(CACHE_DURATION)));
        return route;
    }

    private Optional<RouteEstimate> request(
            TransportType transportType,
            Location start,
            Location end
    ) {
        try {
            URI uri = transportType == TransportType.CAR
                    ? carUri(start, end)
                    : walkUri(start, end);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(requestTimeout)
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return Optional.empty();
            return transportType == TransportType.CAR
                    ? parseCar(response.body())
                    : parseWalk(response.body());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private URI carUri(Location start, Location end) {
        return URI.create(CAR_API_URL
                + "?origin=" + start.longitude() + "," + start.latitude()
                + "&destination=" + end.longitude() + "," + end.latitude()
                + "&priority=RECOMMEND&summary=true");
    }

    private URI walkUri(Location start, Location end) {
        return URI.create(WALK_API_URL
                + "?start_x=" + start.longitude()
                + "&start_y=" + start.latitude()
                + "&end_x=" + end.longitude()
                + "&end_y=" + end.latitude());
    }

    Optional<RouteEstimate> parseCar(String responseBody) throws Exception {
        JsonNode route = objectMapper.readTree(responseBody).path("routes").path(0);
        if (route.isMissingNode() || route.path("result_code").asInt(-1) != 0) {
            return Optional.empty();
        }
        JsonNode summary = route.path("summary");
        return estimate(summary.path("duration").asInt(), summary.path("distance").asInt());
    }

    Optional<RouteEstimate> parseWalk(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!"OK".equals(root.path("status").asText())) return Optional.empty();
        JsonNode properties = root.path("routes").path(0).path("properties");
        if (properties.isMissingNode()) return Optional.empty();
        return estimate(
                properties.path("totalTime").asInt(),
                properties.path("totalDistance").asInt());
    }

    private Optional<RouteEstimate> estimate(int seconds, int distanceMeters) {
        if (seconds <= 0 || distanceMeters <= 0) return Optional.empty();
        return Optional.of(new RouteEstimate(
                Math.max(1, (int) Math.ceil(seconds / 60.0)),
                distanceMeters));
    }

    public record Location(double latitude, double longitude) {}
    public record RouteEstimate(int totalMinutes, int totalDistanceMeters) {}
    private record CacheEntry(Optional<RouteEstimate> route, Instant expiresAt) {}
}
