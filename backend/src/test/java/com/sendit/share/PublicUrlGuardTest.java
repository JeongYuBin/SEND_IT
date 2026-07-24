package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class PublicUrlGuardTest {

    private final PublicUrlGuard guard = new PublicUrlGuard();

    @Test
    void blocksLoopbackAndPrivateAddresses() {
        assertThatThrownBy(() -> guard.validate(URI.create("http://127.0.0.1/admin")))
                .isInstanceOf(ContentAnalysisException.class);
        assertThatThrownBy(() -> guard.validate(URI.create("http://10.0.0.1/admin")))
                .isInstanceOf(ContentAnalysisException.class);
        assertThatThrownBy(() -> guard.validate(URI.create("http://169.254.169.254/latest/meta-data")))
                .isInstanceOf(ContentAnalysisException.class);
    }
}

