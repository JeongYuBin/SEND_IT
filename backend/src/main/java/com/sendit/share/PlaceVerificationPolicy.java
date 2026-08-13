package com.sendit.share;

import org.springframework.stereotype.Component;

@Component
public class PlaceVerificationPolicy {
    private static final java.util.Set<String> GENERIC_CONTENT_NAMES = java.util.Set.of(
            "또간집", "맛집", "먹방", "브이로그", "데이트", "여행", "핫플", "핫플레이스");
    private static final double KOREA_MIN_LATITUDE = 32.0;
    private static final double KOREA_MAX_LATITUDE = 39.5;
    private static final double KOREA_MIN_LONGITUDE = 123.0;
    private static final double KOREA_MAX_LONGITUDE = 132.0;

    public boolean isVerified(PageMetadata metadata) {
        if (metadata == null || isBlank(metadata.placeName())) return false;
        if (!hasValidKoreanCoordinates(metadata.latitude(), metadata.longitude())) return false;
        return isPlausiblePlaceName(metadata.placeName());
    }

    private boolean hasValidKoreanCoordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= KOREA_MIN_LATITUDE && latitude <= KOREA_MAX_LATITUDE
                && longitude >= KOREA_MIN_LONGITUDE && longitude <= KOREA_MAX_LONGITUDE;
    }

    private boolean isPlausiblePlaceName(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() < 2 || normalized.length() > 80) return false;
        if (GENERIC_CONTENT_NAMES.contains(normalized.toLowerCase(java.util.Locale.KOREAN))) return false;
        return !normalized.matches("(?i).*(https?://|www\\.|#\\S+).*");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
