package com.sendit.share;

public record PageMetadata(
        String title,
        String description,
        String imageUrl,
        String placeName,
        String category,
        String address,
        Double latitude,
        Double longitude
) {
}
