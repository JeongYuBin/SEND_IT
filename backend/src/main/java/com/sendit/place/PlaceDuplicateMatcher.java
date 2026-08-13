package com.sendit.place;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PlaceDuplicateMatcher {
    private static final double NEARBY_METERS = 150.0;
    private final PlaceRepository places;

    public PlaceDuplicateMatcher(PlaceRepository places) {
        this.places = places;
    }

    public Optional<Place> find(String normalizedName, SavedPlaceDtos.CreateRequest request) {
        if (hasText(request.kakaoPlaceId())) {
            var matched = places.findFirstByKakaoPlaceId(request.kakaoPlaceId());
            if (matched.isPresent()) return matched;
        }
        if (hasText(request.tourismContentId())) {
            var matched = places.findFirstByTourismContentId(request.tourismContentId());
            if (matched.isPresent()) return matched;
        }
        if (request.latitude() != null && request.longitude() != null) {
            var matched = places.findNearbyDuplicate(
                    normalizedName, request.latitude(), request.longitude(), NEARBY_METERS);
            if (matched.isPresent()) return matched;
        }
        if (hasText(request.roadAddress())) {
            var matched = places.findFirstByNormalizedNameAndRoadAddress(
                    normalizedName, request.roadAddress());
            if (matched.isPresent()) return matched;
        }
        if (hasText(request.address())) {
            var matched = places.findFirstByNormalizedNameAndAddress(
                    normalizedName, request.address());
            if (matched.isPresent()) return matched;
        }
        return places.findFirstByNormalizedNameAndLatitudeAndLongitude(
                normalizedName, request.latitude(), request.longitude());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
