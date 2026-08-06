package com.sendit.share;

public record StoredMedia(
        String storageKey,
        String originalFilename,
        String contentType,
        long sizeBytes
) {
}

