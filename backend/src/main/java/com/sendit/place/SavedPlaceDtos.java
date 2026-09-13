package com.sendit.place;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.sendit.share.SourceType;

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
            @Size(max=50) String tourismContentId,
            @Size(max=20) String tourismContentTypeId,
            LocalDate eventStartDate,
            LocalDate eventEndDate,
            Long sharedContentId,
            @NotNull Long collectionId,
            @Size(max=1000) String memo,
            @Min(0) @Max(5) Integer priority,
            @Size(max=50) String kakaoPlaceId,
            @Size(max=50) String phone,
            @Size(max=2048) String kakaoPlaceUrl
    ) {}
    public record UpdateRequest(
            @Size(max=200) String name,
            @Size(max=100) String category,
            @Size(max=500) String address,
            @Size(max=500) String roadAddress,
            @Size(max=2048) String imageUrl,
            @Size(max=1000) String memo,
            @Min(0) @Max(5) Integer priority,
            Long collectionId,
            Boolean clearCollection
    ) {}
    public record Response(
            Long savedPlaceId, Long placeId, String name, String category,
            String address, String roadAddress, Double latitude, Double longitude,
            String description, String imageUrl, String phone, String homepageUrl,
            String tourismContentId, String tourismContentTypeId,
            String operatingHours, String restDays, String parkingInfo,
            LocalDate eventStartDate, LocalDate eventEndDate,
            Long collectionId, String collectionName,
            String memo, int priority, Instant savedAt,
            String originalUrl, String kakaoPlaceId, String kakaoPlaceUrl,
            List<SourceResponse> sources
    ) {}
    public record SourceResponse(
            Long sharedContentId,
            SourceType sourceType,
            String title,
            String description,
            String originalUrl,
            String thumbnailUrl,
            Instant linkedAt
    ) {}
}
