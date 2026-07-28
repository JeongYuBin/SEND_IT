package com.sendit.share;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VisitKoreaMetadataClient {

    private static final String HOST = "korean.visitkorea.or.kr";
    private static final URI DETAIL_API = URI.create("https://" + HOST + "/call");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public VisitKoreaMetadataClient(
            ObjectMapper objectMapper,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
    }

    public PageMetadata enrich(String pageUrl, PageMetadata fallback) {
        String contentId = contentId(pageUrl);
        if (contentId == null) {
            return fallback;
        }

        try {
            String form = "cmd=TOUR_CONTENT_BODY_DETAIL&cotId="
                    + URLEncoder.encode(contentId, StandardCharsets.UTF_8)
                    + "&locationx=&locationy=&stampId=";
            HttpRequest request = HttpRequest.newBuilder(DETAIL_API)
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "SEND-IT metadata analyzer")
                    .header("Referer", "https://" + HOST + "/")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallback;
            }
            return merge(fallback, response.body());
        } catch (Exception ignored) {
            // 제공자별 보강 실패가 기본 HTML 분석 결과까지 실패시키지는 않는다.
            return fallback;
        }
    }

    PageMetadata merge(PageMetadata fallback, String responseBody) throws Exception {
        JsonNode detail = objectMapper.readTree(responseBody).path("body").path("detail");
        if (!detail.isObject()) {
            return fallback;
        }

        return new PageMetadata(
                firstNonBlank(text(detail, "title"), fallback.title()),
                firstNonBlank(text(detail, "overView"), fallback.description()),
                fallback.imageUrl(),
                firstNonBlank(text(detail, "title"), fallback.placeName()),
                firstNonBlank(text(detail, "cat3Nm"), text(detail, "cat2SubNm"),
                        text(detail, "cat2Nm"), text(detail, "cat1Nm"), fallback.category()),
                firstNonBlank(text(detail, "addr1"), fallback.address()),
                firstNonNull(number(detail, "mapY"), fallback.latitude()),
                firstNonNull(number(detail, "mapX"), fallback.longitude())
        );
    }

    private String contentId(String pageUrl) {
        try {
            URI uri = URI.create(pageUrl);
            if (!HOST.equalsIgnoreCase(uri.getHost()) || !"/detail/ms_detail.do".equals(uri.getPath())) {
                return null;
            }
            for (String parameter : uri.getRawQuery() == null ? new String[0] : uri.getRawQuery().split("&")) {
                String[] pair = parameter.split("=", 2);
                if (pair.length == 2 && "cotid".equalsIgnoreCase(pair[0])) {
                    String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    return value.isBlank() ? null : value;
                }
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Double number(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
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
}
