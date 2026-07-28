package com.sendit.share;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ShareDtos {

    private ShareDtos() {
    }

    public record CreateShareRequest(
            @NotBlank @Size(max = 2048) String url,
            SourceType sourceType,
            @Size(max = 10000) String sharedText
    ) {
    }

    public record ShareAcceptedResponse(
            Long shareId,
            AnalysisStatus status,
            String message,
            boolean duplicate
    ) {
    }

    public record ShareDetailResponse(
            Long shareId,
            String originalUrl,
            SourceType sourceType,
            String sharedText,
            String title,
            String description,
            String thumbnailUrl,
            AnalysisStatus status,
            String analysisError,
            String extractedPlaceName,
            String extractedCategory,
            String extractedAddress,
            Double extractedLatitude,
            Double extractedLongitude,
            Instant createdAt
    ) {
    }
}
