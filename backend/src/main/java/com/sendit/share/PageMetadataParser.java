package com.sendit.share;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class PageMetadataParser {

    public PageMetadata parse(String html, String baseUrl) {
        Document document = Jsoup.parse(html, baseUrl);
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
        return new PageMetadata(blankToNull(title), blankToNull(description), blankToNull(image));
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
}

