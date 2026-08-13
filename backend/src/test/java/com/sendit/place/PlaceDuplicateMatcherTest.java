package com.sendit.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlaceDuplicateMatcherTest {
    private final PlaceRepository places = mock(PlaceRepository.class);
    private final PlaceDuplicateMatcher matcher = new PlaceDuplicateMatcher(places);

    @Test
    void prioritizesKakaoIdentityOverNameAndCoordinates() {
        Place expected = mock(Place.class);
        when(places.findFirstByKakaoPlaceId("123")).thenReturn(Optional.of(expected));

        var result = matcher.find("테스트", request("123", 37.5, 127.0));

        assertThat(result).contains(expected);
    }

    @Test
    void mergesSameNormalizedNameWithinOneHundredFiftyMeters() {
        Place expected = mock(Place.class);
        when(places.findNearbyDuplicate("테스트", 37.5, 127.0, 150.0))
                .thenReturn(Optional.of(expected));

        var result = matcher.find("테스트", request(null, 37.5, 127.0));

        assertThat(result).contains(expected);
    }

    private SavedPlaceDtos.CreateRequest request(
            String kakaoId, Double latitude, Double longitude) {
        return new SavedPlaceDtos.CreateRequest(
                "테스트", "장소", null, null, latitude, longitude,
                null, null, null, null, null, null, null, null,
                null, 0, kakaoId, null, null);
    }
}
