package com.sendit.tourism;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendit.share.PageMetadata;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TourApiClient {

    private static final String DEFAULT_BASE_URL =
            "https://apis.data.go.kr/B551011/KorService2";
    private static final Duration OPERATING_INFO_CACHE_DURATION = Duration.ofHours(6);
    private static final Pattern TIME_RANGE = Pattern.compile(
            "(\\d{1,2})\\s*:?\\s*(\\d{2})\\s*[~～\\-]\\s*(\\d{1,2})\\s*:?\\s*(\\d{2})");
    private static final Map<String, String> CONTENT_TYPE_LABELS = Map.of(
            "12", "관광지",
            "14", "문화시설",
            "15", "축제·공연·행사",
            "25", "여행코스",
            "28", "레포츠",
            "32", "숙박",
            "38", "쇼핑",
            "39", "음식점"
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final String serviceKey;
    private final String baseUrl;
    private final ConcurrentHashMap<String, OperatingInfoCacheEntry> operatingInfoCache =
            new ConcurrentHashMap<>();

    @Autowired
    public TourApiClient(
            ObjectMapper objectMapper,
            @Value("${app.tour-api.service-key:}") String serviceKey,
            @Value("${app.tour-api.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        this(objectMapper, serviceKey, baseUrl,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                        .build(),
                Duration.ofSeconds(requestTimeoutSeconds));
    }

    TourApiClient(ObjectMapper objectMapper, String serviceKey, String baseUrl,
                  HttpClient httpClient, Duration requestTimeout) {
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    public PageMetadata enrich(PageMetadata fallback) {
        String keyword = searchKeyword(fallback);
        if (serviceKey.isBlank() || keyword == null) {
            return fallback;
        }

        try {
            String searchBody = get("/searchKeyword2", Map.of(
                    "keyword", keyword,
                    "numOfRows", "10",
                    "pageNo", "1"
            ));
            TourItem match = bestMatch(fallback, items(searchBody));
            if (match == null) {
                return fallback;
            }
            String detailBody = get("/detailCommon2", Map.of(
                    "contentId", match.contentId(),
                    "contentTypeId", match.contentTypeId(),
                    "defaultYN", "Y",
                    "firstImageYN", "Y",
                    "addrinfoYN", "Y",
                    "mapinfoYN", "Y",
                    "overviewYN", "Y",
                    "numOfRows", "1",
                    "pageNo", "1"
            ));
            TourItem detail = items(detailBody).stream().findFirst().orElse(match);
            return merge(fallback, match, detail);
        } catch (Exception ignored) {
            // 관광공사 API 장애가 원본 URL 분석과 저장을 막지 않도록 기존 결과를 사용한다.
            return fallback;
        }
    }

    public Optional<OperatingInfo> operatingInfo(String placeName, String address) {
        if (serviceKey.isBlank() || placeName == null || placeName.isBlank()) {
            return Optional.empty();
        }
        String cacheKey = normalize(placeName) + ":" + normalize(address);
        OperatingInfoCacheEntry cached = operatingInfoCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.info();
        }
        Optional<OperatingInfo> result;
        try {
            PageMetadata fallback = new PageMetadata(
                    placeName, null, null, placeName, null, address, null, null);
            TourItem match = bestMatch(fallback, items(get("/searchKeyword2", Map.of(
                    "keyword", placeName,
                    "numOfRows", "10",
                    "pageNo", "1"
            ))));
            if (match == null) {
                result = Optional.empty();
            } else {
                String body = get("/detailIntro2", Map.of(
                        "contentId", match.contentId(),
                        "contentTypeId", match.contentTypeId(),
                        "numOfRows", "1",
                        "pageNo", "1"
                ));
                JsonNode item = firstItem(body);
                String hours = cleanOverview(firstText(item,
                        "usetime", "usetimeculture", "usetimeleports",
                        "opentimefood", "checkintime"));
                String restDays = cleanOverview(firstText(item,
                        "restdate", "restdateculture", "restdateleports",
                        "restdatefood", "restdateaccommodation"));
                result = hours == null && restDays == null
                        ? Optional.empty()
                        : Optional.of(new OperatingInfo(
                                hours, restDays, parseTimeRange(hours)));
            }
        } catch (Exception ignored) {
            result = Optional.empty();
        }
        operatingInfoCache.put(cacheKey, new OperatingInfoCacheEntry(
                result, Instant.now().plus(OPERATING_INFO_CACHE_DURATION)));
        return result;
    }

    private String get(String path, Map<String, String> parameters) throws Exception {
        StringBuilder query = new StringBuilder()
                .append("serviceKey=").append(encode(serviceKey))
                .append("&MobileOS=ETC")
                .append("&MobileApp=SEND_IT")
                .append("&_type=json");
        parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> query.append('&')
                        .append(encode(entry.getKey()))
                        .append('=')
                        .append(encode(entry.getValue())));
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(baseUrl + path + "?" + query))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("TourAPI HTTP " + response.statusCode());
        }
        return response.body();
    }

    List<TourItem> items(String responseBody) throws Exception {
        JsonNode response = objectMapper.readTree(responseBody).path("response");
        String resultCode = response.path("header").path("resultCode").asText("");
        if (!resultCode.isBlank() && !"0000".equals(resultCode)) {
            throw new IllegalStateException("TourAPI resultCode " + resultCode);
        }
        JsonNode itemNode = response.path("body").path("items").path("item");
        List<TourItem> result = new ArrayList<>();
        if (itemNode.isArray()) {
            itemNode.forEach(node -> result.add(item(node)));
        } else if (itemNode.isObject()) {
            result.add(item(itemNode));
        }
        return result;
    }

    private JsonNode firstItem(String responseBody) throws Exception {
        JsonNode item = objectMapper.readTree(responseBody)
                .path("response").path("body").path("items").path("item");
        if (item.isArray()) return item.path(0);
        return item;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private TimeRange parseTimeRange(String value) {
        if (value == null) return null;
        Matcher matcher = TIME_RANGE.matcher(value);
        if (!matcher.find()) return null;
        try {
            return new TimeRange(
                    LocalTime.of(Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2))),
                    LocalTime.of(Integer.parseInt(matcher.group(3)),
                            Integer.parseInt(matcher.group(4))));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    TourItem bestMatch(PageMetadata fallback, List<TourItem> candidates) {
        String expectedName = normalize(searchKeyword(fallback));
        String expectedRegion = firstAddressPart(fallback.address());
        return candidates.stream()
                .map(candidate -> new ScoredItem(candidate,
                        matchScore(expectedName, expectedRegion, candidate)))
                .filter(scored -> scored.score() >= 70)
                .max(Comparator.comparingInt(ScoredItem::score))
                .map(ScoredItem::item)
                .orElse(null);
    }

    PageMetadata merge(PageMetadata fallback, TourItem search, TourItem detail) {
        String contentTypeId = firstNonBlank(detail.contentTypeId(), search.contentTypeId());
        return new PageMetadata(
                fallback.title(),
                firstNonBlank(cleanOverview(detail.overview()), fallback.description()),
                firstNonBlank(detail.firstImage(), search.firstImage(), fallback.imageUrl()),
                firstNonBlank(detail.title(), search.title(), fallback.placeName()),
                firstNonBlank(categoryLabel(contentTypeId), fallback.category()),
                firstNonBlank(fullAddress(detail), fullAddress(search), fallback.address()),
                firstNonNull(detail.latitude(), search.latitude(), fallback.latitude()),
                firstNonNull(detail.longitude(), search.longitude(), fallback.longitude())
        );
    }

    private TourItem item(JsonNode node) {
        return new TourItem(
                text(node, "contentid"),
                text(node, "contenttypeid"),
                text(node, "title"),
                text(node, "addr1"),
                text(node, "addr2"),
                text(node, "mapy"),
                text(node, "mapx"),
                text(node, "firstimage"),
                text(node, "overview")
        );
    }

    private int matchScore(String expectedName, String expectedRegion, TourItem candidate) {
        String candidateName = normalize(candidate.title());
        int score;
        if (candidateName.equals(expectedName)) {
            score = 100;
        } else if (!candidateName.isBlank()
                && (candidateName.contains(expectedName) || expectedName.contains(candidateName))) {
            score = 70;
        } else {
            score = 0;
        }
        if (expectedRegion != null && fullAddress(candidate) != null
                && fullAddress(candidate).startsWith(expectedRegion)) {
            score += 10;
        }
        return score;
    }

    private String searchKeyword(PageMetadata metadata) {
        String value = firstNonBlank(metadata.placeName(), metadata.title());
        if (value == null) {
            return null;
        }
        String cleaned = value.split("[|>｜]")[0]
                .replaceAll("\\s*[-–—:]\\s*(여행|관광|맛집|카페|대한민국).*", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? null : cleaned.substring(0, Math.min(cleaned.length(), 100));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.KOREAN)
                .replaceAll("<[^>]+>", "")
                .replaceAll("[^0-9a-z가-힣]", "");
    }

    private String firstAddressPart(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        return address.trim().split("\\s+")[0];
    }

    private String fullAddress(TourItem item) {
        return firstNonBlank(
                join(item.address1(), item.address2()),
                item.address1(),
                item.address2()
        );
    }

    private String join(String first, String second) {
        return first == null || second == null ? null : first + " " + second;
    }

    private String cleanOverview(String overview) {
        return overview == null ? null : Jsoup.parse(overview).text().trim();
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String categoryLabel(String contentTypeId) {
        return contentTypeId == null ? null : CONTENT_TYPE_LABELS.get(contentTypeId);
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    record TourItem(
            String contentId,
            String contentTypeId,
            String title,
            String address1,
            String address2,
            String latitudeText,
            String longitudeText,
            String firstImage,
            String overview
    ) {
        Double latitude() {
            return parse(latitudeText);
        }

        Double longitude() {
            return parse(longitudeText);
        }

        private static Double parse(String value) {
            try {
                return value == null ? null : Double.valueOf(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    private record ScoredItem(TourItem item, int score) {}
    public record OperatingInfo(String hours, String restDays, TimeRange timeRange) {}
    public record TimeRange(LocalTime opensAt, LocalTime closesAt) {}
    private record OperatingInfoCacheEntry(
            Optional<OperatingInfo> info,
            Instant expiresAt
    ) {}
}
