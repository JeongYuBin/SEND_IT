package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceVerificationPolicyTest {
    private final PlaceVerificationPolicy policy = new PlaceVerificationPolicy();

    @Test
    void requiresAddressOrCoordinatesInAdditionToPlaceName() {
        assertThat(policy.isVerified(metadata("해변카페", null, null, null))).isFalse();
        assertThat(policy.isVerified(metadata("해변카페", "강원 속초시 해오름로 1", null, null)))
                .isTrue();
        assertThat(policy.isVerified(metadata("해변카페", null, 38.1, 128.6))).isTrue();
    }

    @Test
    void rejectsCoordinatesWithoutPlaceName() {
        assertThat(policy.isVerified(metadata(null, null, 38.1, 128.6))).isFalse();
    }

    private PageMetadata metadata(String name, String address, Double latitude, Double longitude) {
        return new PageMetadata(null, null, null, name, null, address, latitude, longitude);
    }
}
