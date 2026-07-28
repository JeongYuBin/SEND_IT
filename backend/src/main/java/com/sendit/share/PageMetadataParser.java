package com.sendit.share;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayDeque;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class PageMetadataParser {

    private final ObjectMapper objectMapper;

    public PageMetadataParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PageMetadata parse(String html, String baseUrl) {
        Document document = Jsoup.parse(html, baseUrl);
        PlaceCandidate place = structuredPlace(document);
        String title = firstNonBlank(
                content(document, "meta[property=og:title]"),
                content(document, "meta[name=twitter:title]"),
                document.title()
        );
        String description = firstNonBlank(
                content(document, "meta[property=og:description]"),
                content(document, "meta[name=description]"),
                content(document, "meta[name=twitter:description]")
        );
        String image = firstNonBlank(
                absoluteContent(document, "meta[property=og:image]"),
                absoluteContent(document, "meta[name=twitter:image]")
        );
        String placeName = firstNonBlank(
                place.name(),
                content(document, "meta[property=place:name]"),
                visitKoreaPlaceName(title, baseUrl)
        );
        String address = firstNonBlank(
                place.address(),
                content(document, "meta[property=business:contact_data:street_address]")
        );
        return new PageMetadata(
                blankToNull(title),
                blankToNull(description),
                blankToNull(image),
                blankToNull(placeName),
                blankToNull(place.category()),
                blankToNull(address),
                place.latitude(),
                place.longitude()
        );
    }

    private PlaceCandidate structuredPlace(Document document) {
        for (var script : document.select("script[type=application/ld+json]")) {
            try {
                PlaceCandidate candidate = findPlace(objectMapper.readTree(script.data()));
                if (candidate.name() != null || candidate.address() != null) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // 잘못된 JSON-LD는 다른 메타데이터 분석을 방해하지 않는다.
            }
        }
        return PlaceCandidate.empty();
    }

    private PlaceCandidate findPlace(JsonNode root) {
        ArrayDeque<JsonNode> nodes = new ArrayDeque<>();
        nodes.add(root);
        while (!nodes.isEmpty()) {
            JsonNode node = nodes.removeFirst();
            if (node.isArray()) {
                node.forEach(nodes::addLast);
                continue;
            }
            if (!node.isObject()) {
                continue;
            }
            if (isPlaceType(node.path("@type"))) {
                return candidate(node);
            }
            node.elements().forEachRemaining(child -> {
                if (child.isContainerNode()) nodes.addLast(child);
            });
        }
        return PlaceCandidate.empty();
    }

    private boolean isPlaceType(JsonNode type) {
        if (type.isArray()) {
            for (JsonNode value : type) {
                if (isPlaceType(value)) return true;
            }
            return false;
        }
        String value = type.asText("");
        return value.equals("Place")
                || value.equals("LocalBusiness")
                || value.equals("TouristAttraction")
                || value.equals("Restaurant")
                || value.equals("CafeOrCoffeeShop")
                || value.equals("LodgingBusiness")
                || value.endsWith("Store");
    }

    private PlaceCandidate candidate(JsonNode node) {
        JsonNode address = node.path("address");
        String formattedAddress = address.isTextual()
                ? address.asText()
                : joinAddress(address);
        JsonNode geo = node.path("geo");
        return new PlaceCandidate(
                text(node, "name"),
                category(node.path("@type")),
                formattedAddress,
                number(geo, "latitude"),
                number(geo, "longitude")
        );
    }

    private String joinAddress(JsonNode address) {
        String joined = String.join(" ", nonBlank(
                text(address, "addressRegion"),
                text(address, "addressLocality"),
                text(address, "streetAddress")
        ));
        return blankToNull(joined);
    }

    private java.util.List<String> nonBlank(String... values) {
        return java.util.Arrays.stream(values).filter(v -> v != null && !v.isBlank()).toList();
    }

    private String category(JsonNode type) {
        if (type.isArray() && !type.isEmpty()) return type.get(0).asText();
        return type.asText(null);
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return blankToNull(value);
    }

    private Double number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isNumber()) return value.doubleValue();
        try {
            return value.isTextual() ? Double.valueOf(value.asText()) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String content(Document document, String selector) {
        var element = document.selectFirst(selector);
        return element == null ? null : element.attr("content").trim();
    }

    private String absoluteContent(Document document, String selector) {
        var element = document.selectFirst(selector);
        return element == null ? null : element.absUrl("content").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String visitKoreaPlaceName(String title, String baseUrl) {
        try {
            if (!"korean.visitkorea.or.kr".equalsIgnoreCase(URI.create(baseUrl).getHost())) {
                return null;
            }
            return blankToNull(title == null
                    ? null
                    : title.replaceFirst("\\s*>\\s*여행지\\s*:\\s*대한민국\\s*구석구석.*$", "").trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record PlaceCandidate(
            String name,
            String category,
            String address,
            Double latitude,
            Double longitude
    ) {
        static PlaceCandidate empty() {
            return new PlaceCandidate(null, null, null, null, null);
        }
    }
}
