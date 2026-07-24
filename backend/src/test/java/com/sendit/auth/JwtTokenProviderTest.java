package com.sendit.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(
            "test-secret-key-that-is-long-enough-for-hmac",
            30,
            14
    );

    @Test
    void issuesUniqueTokensForSameUserWithinSameSecond() {
        String first = provider.createRefreshToken(1L, "test@example.com");
        String second = provider.createRefreshToken(1L, "test@example.com");

        assertThat(first).isNotEqualTo(second);
    }
}

