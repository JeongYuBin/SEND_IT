package com.sendit.share;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SafePageFetcher {

    private static final int MAX_REDIRECTS = 3;

    private final PublicUrlGuard publicUrlGuard;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final int maxResponseBytes;

    public SafePageFetcher(
            PublicUrlGuard publicUrlGuard,
            @Value("${app.analysis.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.analysis.request-timeout-seconds}") long requestTimeoutSeconds,
            @Value("${app.analysis.max-response-bytes}") int maxResponseBytes
    ) {
        this.publicUrlGuard = publicUrlGuard;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
        this.maxResponseBytes = maxResponseBytes;
    }

    public FetchedPage fetch(String url) {
        URI uri = URI.create(url);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            publicUrlGuard.validate(uri);
            HttpResponse<InputStream> response = send(uri);
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new ContentAnalysisException("리다이렉트 주소가 없습니다."));
                uri = uri.resolve(location);
                continue;
            }
            if (status < 200 || status >= 300) {
                throw new ContentAnalysisException("원본 페이지 응답 오류: HTTP " + status);
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.toLowerCase().contains("text/html")) {
                throw new ContentAnalysisException("HTML 페이지가 아닙니다.");
            }
            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(maxResponseBytes + 1);
                if (bytes.length > maxResponseBytes) {
                    throw new ContentAnalysisException("페이지 크기가 분석 제한을 초과했습니다.");
                }
                return new FetchedPage(uri.toString(), new String(bytes, StandardCharsets.UTF_8));
            } catch (IOException exception) {
                throw new ContentAnalysisException("페이지 내용을 읽지 못했습니다.");
            }
        }
        throw new ContentAnalysisException("리다이렉트 횟수가 너무 많습니다.");
    }

    private HttpResponse<InputStream> send(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("User-Agent", "SEND-IT-MetadataBot/0.1")
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException exception) {
            throw new ContentAnalysisException("원본 페이지에 연결하지 못했습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ContentAnalysisException("페이지 분석이 중단되었습니다.");
        }
    }

    public record FetchedPage(String finalUrl, String html) {
    }
}

