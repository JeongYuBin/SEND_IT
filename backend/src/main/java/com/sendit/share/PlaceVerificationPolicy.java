package com.sendit.share;

import org.springframework.stereotype.Component;

@Component
public class PlaceVerificationPolicy {
    public boolean isVerified(PageMetadata metadata) {
        if (metadata == null || isBlank(metadata.placeName())) return false;
        boolean hasCoordinates = metadata.latitude() != null && metadata.longitude() != null;
        return hasCoordinates || !isBlank(metadata.address());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
