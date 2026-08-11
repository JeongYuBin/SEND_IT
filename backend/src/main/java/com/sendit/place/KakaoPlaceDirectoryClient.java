package com.sendit.place;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KakaoPlaceDirectoryClient {
    private static final String DEFAULT_URL =
            "https://dapi.kakao.com/v2/local/search/keyword.json";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final Duration requestTimeout;

    @Autowired
    public KakaoPlaceDirectoryClient(
            ObjectMapper objectMapper,
            @Value("${app.kakao.rest-api-key:}") String apiKey,
            @Value("${app.kakao.place-search-url:" + DEFAULT_URL + "}") String baseUrl,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        this(objectMapper, apiKey, baseUrl,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds)).build(),
                Duration.ofSeconds(requestTimeoutSeconds));
    }

    KakaoPlaceDirectoryClient(ObjectMapper objectMapper, String apiKey, String baseUrl,
                              HttpClient httpClient, Duration requestTimeout) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    public PlaceSearchDtos.Response search(String query, int page) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("카카오 REST API 키가 설정되지 않았습니다.");
        }
        try {
            URI uri = URI.create(baseUrl + "?size=15&page=" + page + "&query="
                    + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(requestTimeout)
                    .header("Authorization", "KakaoAK " + apiKey)
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("카카오 장소 검색을 완료하지 못했습니다.");
            }
            return parse(response.body(), page);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("카카오 장소 검색이 중단되었습니다.", exception);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("카카오 장소 검색을 완료하지 못했습니다.", exception);
        }
    }

    PlaceSearchDtos.Response parse(String body, int page) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        var places = new ArrayList<PlaceSearchDtos.Result>();
        for (JsonNode document : root.path("documents")) {
            places.add(new PlaceSearchDtos.Result(
                    text(document, "id"),
                    text(document, "place_name"),
                    text(document, "category_name"),
                    text(document, "category_group_name"),
                    text(document, "address_name"),
                    text(document, "road_address_name"),
                    text(document, "phone"),
                    number(document, "y"),
                    number(document, "x"),
                    text(document, "place_url")
            ));
        }
        return new PlaceSearchDtos.Response(
                places, page, root.path("meta").path("is_end").asBoolean(true));
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Double number(JsonNode node, String field) {
        try {
            String value = text(node, field);
            return value == null ? null : Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
