package com.sendit.place;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class SavedPlaceDtos {
    private SavedPlaceDtos() {}
    public record CreateRequest(
            @NotBlank @Size(max=200) String name,
            @Size(max=100) String category,
            @Size(max=500) String address,
            @Size(max=500) String roadAddress,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Size(max=5000) String description,
            @Size(max=2048) String imageUrl,
            Long sharedContentId,
            Long collectionId,
            @Size(max=1000) String memo,
            @Min(0) @Max(5) Integer priority
    ) {}
    public record UpdateRequest(
            @Size(max=1000) String memo,
            VisitStatus visitStatus,
            @Min(0) @Max(5) Integer priority,
            Long collectionId
    ) {}
    public record Response(
            Long savedPlaceId, Long placeId, String name, String category,
            String address, String roadAddress, Double latitude, Double longitude,
            String description, String imageUrl, Long collectionId, String collectionName,
            String memo, VisitStatus visitStatus, int priority, Instant savedAt,
            String originalUrl
    ) {}
}
