package com.sendit.share;

import java.util.List;

public record MediaProcessingResult(
        double durationSeconds,
        List<String> frameStorageKeys,
        String audioStorageKey
) {
}

