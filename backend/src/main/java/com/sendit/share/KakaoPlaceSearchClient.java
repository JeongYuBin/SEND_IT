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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KakaoPlaceSearchClient {
    private static final String DEFAULT_BASE_URL =
            "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final Pattern HASHTAG = Pattern.compile("#([가-힣]{2,12})");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final Duration requestTimeout;

    @Autowired
    public KakaoPlaceSearchClient(
            ObjectMapper objectMapper,
            @Value("${app.kakao.rest-api-key:}") String apiKey,
            @Value("${app.kakao.place-search-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        this(objectMapper, apiKey, baseUrl,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                        .build(),
                Duration.ofSeconds(requestTimeoutSeconds));
    }

    KakaoPlaceSearchClient(ObjectMapper objectMapper, String apiKey, String baseUrl,
                           HttpClient httpClient, Duration requestTimeout) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    public PageMetadata enrich(PageMetadata fallback) {
        if (apiKey.isBlank() || fallback.placeName() == null
                || fallback.placeName().isBlank()) return fallback;
        for (String query : searchQueries(fallback)) {
            Optional<PageMetadata> result = search(query, fallback);
            if (result.isPresent()) return result.get();
        }
        return fallback;
    }

    private Optional<PageMetadata> search(String query, PageMetadata fallback) {
        try {
            URI uri = URI.create(baseUrl + "?size=10&query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(requestTimeout)
                    .header("Authorization", "KakaoAK " + apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) return Optional.empty();
            return parse(response.body(), fallback);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private List<String> searchQueries(PageMetadata metadata) {
        Set<String> queries = new LinkedHashSet<>();
        String region = addressRegion(metadata.address());
        if (region != null) queries.add(metadata.placeName() + " " + region);
        queries.add(metadata.placeName());
        String source = first(metadata.description(), metadata.title());
        if (source == null) return List.copyOf(queries);
        Matcher matcher = HASHTAG.matcher(source);
        while (matcher.find() && queries.size() < 4) {
            String tag = matcher.group(1);
            String hashtagRegion = tag.endsWith("맛집")
                    ? tag.substring(0, tag.length() - 2) : tag;
            if (hashtagRegion.length() >= 2 && (tag.endsWith("맛집")
                    || hashtagRegion.matches(".*(시|군|구|동|읍|면|리)$"))) {
                queries.add(metadata.placeName() + " " + hashtagRegion);
            }
        }
        return List.copyOf(queries);
    }

    Optional<PageMetadata> parse(String body, PageMetadata fallback) {
        try {
            JsonNode documents = objectMapper.readTree(body).path("documents");
            if (!documents.isArray()) return Optional.empty();
            String expected = normalize(fallback.placeName());
            JsonNode match = java.util.stream.StreamSupport.stream(documents.spliterator(), false)
                    .map(document -> new ScoredDocument(document,
                            score(expected, normalize(text(document, "place_name")),
                                    fallback.address(), candidateAddress(document))))
                    .filter(candidate -> candidate.score() >= 80)
                    .max(Comparator.comparingInt(ScoredDocument::score))
                    .map(ScoredDocument::document)
                    .orElse(null);
            if (match == null) return Optional.empty();
            return Optional.of(new PageMetadata(
                    fallback.title(),
                    fallback.description(),
                    fallback.imageUrl(),
                    first(text(match, "place_name"), fallback.placeName()),
                    first(text(match, "category_group_name"), fallback.category()),
                    first(text(match, "road_address_name"), text(match, "address_name"),
                            fallback.address()),
                    number(match, "y"),
                    number(match, "x")
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private int score(String expected, String candidate,
                      String expectedAddress, String candidateAddress) {
        int nameScore;
        if (candidate.equals(expected)) nameScore = 100;
        else if (!candidate.isBlank()
                && (candidate.contains(expected) || expected.contains(candidate))) nameScore = 80;
        else return 0;
        String region = addressRegion(expectedAddress);
        if (region != null && candidateAddress != null
                && normalize(candidateAddress).contains(normalize(region))) return nameScore + 20;
        return nameScore;
    }

    private String candidateAddress(JsonNode document) {
        return first(text(document, "road_address_name"), text(document, "address_name"));
    }

    private String addressRegion(String address) {
        if (address == null || address.isBlank()) return null;
        String[] tokens = address.trim().split("\\s+");
        if (tokens.length < 2) return null;
        return tokens[0] + " " + tokens[1];
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.KOREAN)
                .replaceAll("[^0-9a-z가-힣]", "");
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

    private String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private record ScoredDocument(JsonNode document, int score) {
    }
}
