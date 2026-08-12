package com.sendit.share;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

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
            String mediaOriginalFilename,
            String mediaContentType,
            Long mediaSizeBytes,
            Double mediaDurationSeconds,
            int mediaFrameCount,
            boolean mediaAudioAvailable,
            String mediaOcrText,
            String mediaTranscript,
            List<ExtractedPlaceResponse> extractedPlaces,
            Instant createdAt
    ) {
    }

    public record ExtractedPlaceResponse(
            Long id,
            int order,
            String name,
            String category,
            String address,
            Double latitude,
            Double longitude,
            String imageUrl,
            Long savedPlaceId
    ) { }

    public record SharePageResponse(
            List<ShareDetailResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
    }
}
