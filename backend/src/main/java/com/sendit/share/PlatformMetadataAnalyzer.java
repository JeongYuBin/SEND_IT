package com.sendit.share;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PlatformMetadataAnalyzer {
    private final List<PlatformMetadataExtractor> extractors;

    public PlatformMetadataAnalyzer(List<PlatformMetadataExtractor> extractors) {
        this.extractors = extractors;
    }

    public Optional<PageMetadata> analyze(String url) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(url))
                .findFirst()
                .flatMap(extractor -> extractor.extract(url));
    }
}
