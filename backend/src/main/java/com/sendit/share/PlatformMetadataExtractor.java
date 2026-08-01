package com.sendit.share;

import java.util.Optional;

public interface PlatformMetadataExtractor {
    boolean supports(String url);
    Optional<PageMetadata> extract(String url);
}
