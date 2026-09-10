package com.sendit.share;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class GoogleShareMetadataExtractor implements PlatformMetadataExtractor {

    private final SafePageFetcher safePageFetcher;

    public GoogleShareMetadataExtractor(SafePageFetcher safePageFetcher) {
        this.safePageFetcher = safePageFetcher;
    }

    @Override
    public boolean supports(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && host.equalsIgnoreCase("share.google");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public Optional<PageMetadata> extract(String url) {
        if (!supports(url)) return Optional.empty();
        try {
            SafePageFetcher.FetchedPage page = safePageFetcher.fetch(url);
            return parse(page.html(), page.finalUrl());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    Optional<PageMetadata> parse(String html, String baseUrl) {
        var document = Jsoup.parse(html, baseUrl);
        for (var link : document.select("a[href*=/search?], a[href*=/maps/search?]")) {
            String query = queryParameter(link.absUrl("href"), "q");
            if (query != null && !query.isBlank()) {
                String placeName = cleanQuery(query);
                if (!placeName.isBlank()) {
                    return Optional.of(new PageMetadata(
                            placeName, null, null, placeName,
                            null, null, null, null));
                }
            }
        }
        return Optional.empty();
    }

    private String queryParameter(String url, String expectedName) {
        try {
            String query = URI.create(url).getRawQuery();
            if (query == null) return null;
            for (String parameter : query.split("&")) {
                String[] pair = parameter.split("=", 2);
                if (pair.length == 2 && expectedName.equals(pair[0])) {
                    return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                }
            }
        } catch (IllegalArgumentException ignored) {
            // 형식이 다른 링크는 다음 후보를 확인한다.
        }
        return null;
    }

    private String cleanQuery(String query) {
        return query.replace('+', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
