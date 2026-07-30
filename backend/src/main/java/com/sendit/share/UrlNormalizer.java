package com.sendit.share;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UrlNormalizer {

    private static final Set<String> TRACKING_PARAMETERS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "fbclid"
    );
    private static final Pattern NAVER_BLOG_POST_PATH =
            Pattern.compile("^/([A-Za-z0-9_-]+)/([0-9]+)$");

    public String normalize(String rawUrl) {
        try {
            URI uri = new URI(rawUrl.trim());
            if (!Set.of("http", "https").contains(uri.getScheme())
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("HTTP 또는 HTTPS URL만 사용할 수 있습니다.");
            }

            String query = normalizeQuery(uri.getRawQuery());
            String path = uri.getPath();
            if (path != null && path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            if ("blog.naver.com".equalsIgnoreCase(uri.getHost())) {
                Matcher post = NAVER_BLOG_POST_PATH.matcher(path == null ? "" : path);
                if (post.matches()) {
                    path = "/PostView.naver";
                    query = "blogId=" + post.group(1) + "&logNo=" + post.group(2);
                }
            }

            return new URI(
                    uri.getScheme().toLowerCase(),
                    uri.getUserInfo(),
                    uri.getHost().toLowerCase(),
                    uri.getPort(),
                    path,
                    query,
                    null
            ).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("올바른 URL 형식이 아닙니다.");
        }
    }

    public SourceType detectSource(String normalizedUrl) {
        String host = URI.create(normalizedUrl).getHost();
        if (host.contains("instagram.com")) {
            return SourceType.INSTAGRAM;
        }
        if (host.contains("youtube.com") || host.equals("youtu.be")) {
            return SourceType.YOUTUBE;
        }
        if (host.contains("blog.naver.com")) {
            return SourceType.NAVER_BLOG;
        }
        if (host.contains("map.") || host.contains("maps.")) {
            return SourceType.MAP;
        }
        return SourceType.WEB;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String normalized = Arrays.stream(query.split("&"))
                .filter(parameter -> {
                    String name = parameter.split("=", 2)[0].toLowerCase();
                    return !TRACKING_PARAMETERS.contains(name);
                })
                .sorted()
                .collect(Collectors.joining("&"));
        return normalized.isBlank() ? null : normalized;
    }
}
