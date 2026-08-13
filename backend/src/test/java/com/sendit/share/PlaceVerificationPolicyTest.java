package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceVerificationPolicyTest {
    private final PlaceVerificationPolicy policy = new PlaceVerificationPolicy();

    @Test
    void requiresValidKoreanCoordinatesInAdditionToPlaceName() {
        assertThat(policy.isVerified(metadata("테스트 카페", null, null, null))).isFalse();
        assertThat(policy.isVerified(metadata(
                "테스트 카페", "강원특별자치도 속초시 해오름로 1", null, null)))
                .isFalse();
        assertThat(policy.isVerified(metadata("테스트 카페", null, 38.1, 128.6))).isTrue();
    }

    @Test
    void rejectsCoordinatesWithoutPlaceName() {
        assertThat(policy.isVerified(metadata(null, null, 38.1, 128.6))).isFalse();
    }

    @Test
    void rejectsInvalidCoordinatesAndUrlLikeNames() {
        assertThat(policy.isVerified(metadata("테스트 카페", null, 91.0, 128.6))).isFalse();
        assertThat(policy.isVerified(metadata("테스트 카페", null, 37.5, 10.0))).isFalse();
        assertThat(policy.isVerified(metadata(
                "https://example.com/place", null, 37.5, 127.0))).isFalse();
    }

    @Test
    void rejectsProgramAndGenericContentNamesAsPhysicalPlaces() {
        assertThat(policy.isVerified(metadata("또간집", null, 37.5, 127.0))).isFalse();
        assertThat(policy.isVerified(metadata("맛집", null, 37.5, 127.0))).isFalse();
    }

    private PageMetadata metadata(
            String name, String address, Double latitude, Double longitude
    ) {
        return new PageMetadata(null, null, null, name, null, address, latitude, longitude);
    }
}
