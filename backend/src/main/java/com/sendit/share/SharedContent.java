package com.sendit.share;

import com.sendit.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "shared_contents")
public class SharedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "normalized_url", nullable = false, length = 2048)
    private String normalizedUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "shared_text", columnDefinition = "text")
    private String sharedText;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "media_storage_key", length = 255)
    private String mediaStorageKey;

    @Column(name = "media_original_filename", length = 500)
    private String mediaOriginalFilename;

    @Column(name = "media_content_type", length = 100)
    private String mediaContentType;

    @Column(name = "media_size_bytes")
    private Long mediaSizeBytes;

    @Column(name = "media_duration_seconds")
    private Double mediaDurationSeconds;

    @Column(name = "media_frame_keys", columnDefinition = "text")
    private String mediaFrameKeys;

    @Column(name = "media_audio_storage_key", length = 255)
    private String mediaAudioStorageKey;

    @Column(name = "media_ocr_text", columnDefinition = "text")
    private String mediaOcrText;

    @Column(name = "media_transcript", columnDefinition = "text")
    private String mediaTranscript;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 30)
    private AnalysisStatus analysisStatus;

    @Column(name = "analysis_error", columnDefinition = "text")
    private String analysisError;

    @Column(name = "extracted_place_name", length = 200)
    private String extractedPlaceName;
    @Column(name = "extracted_category", length = 100)
    private String extractedCategory;
    @Column(name = "extracted_address", length = 500)
    private String extractedAddress;
    @Column(name = "extracted_latitude")
    private Double extractedLatitude;
    @Column(name = "extracted_longitude")
    private Double extractedLongitude;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SharedContent() {
    }

    public SharedContent(
            User user,
            String originalUrl,
            String normalizedUrl,
            SourceType sourceType,
            String sharedText
    ) {
        this.user = user;
        this.originalUrl = originalUrl;
        this.normalizedUrl = normalizedUrl;
        this.sourceType = sourceType;
        this.sharedText = sharedText;
        this.analysisStatus = AnalysisStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSharedText() {
        return sharedText;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getMediaStorageKey() { return mediaStorageKey; }
    public String getMediaOriginalFilename() { return mediaOriginalFilename; }
    public String getMediaContentType() { return mediaContentType; }
    public Long getMediaSizeBytes() { return mediaSizeBytes; }
    public Double getMediaDurationSeconds() { return mediaDurationSeconds; }
    public int getMediaFrameCount() {
        return mediaFrameKeys == null || mediaFrameKeys.isBlank()
                ? 0 : mediaFrameKeys.split("\\n").length;
    }
    public boolean hasMediaAudio() { return mediaAudioStorageKey != null; }
    public boolean hasProcessedMedia() { return getMediaFrameCount() > 0; }
    public String getMediaOcrText() { return mediaOcrText; }
    public String getMediaTranscript() { return mediaTranscript; }
    public String getMediaAudioStorageKey() { return mediaAudioStorageKey; }

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public String getAnalysisError() {
        return analysisError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public String getExtractedPlaceName() { return extractedPlaceName; }
    public String getExtractedCategory() { return extractedCategory; }
    public String getExtractedAddress() { return extractedAddress; }
    public Double getExtractedLatitude() { return extractedLatitude; }
    public Double getExtractedLongitude() { return extractedLongitude; }

    public void queueForAnalysis() {
        analysisStatus = AnalysisStatus.PENDING;
        analysisError = null;
    }

    public void attachMedia(StoredMedia media) {
        mediaStorageKey = media.storageKey();
        mediaOriginalFilename = media.originalFilename();
        mediaContentType = media.contentType();
        mediaSizeBytes = media.sizeBytes();
    }

    public void requireConfirmation() {
        analysisStatus = AnalysisStatus.NEEDS_CONFIRMATION;
    }

    public void attachMediaProcessingResult(MediaProcessingResult result) {
        mediaDurationSeconds = result.durationSeconds();
        mediaFrameKeys = String.join("\n", result.frameStorageKeys());
        mediaAudioStorageKey = result.audioStorageKey();
    }

    public void attachMediaOcrText(String ocrText) {
        mediaOcrText = ocrText;
    }

    public void attachMediaTranscript(String transcript) {
        mediaTranscript = transcript;
    }

    public java.util.List<String> getMediaFrameKeys() {
        return mediaFrameKeys == null || mediaFrameKeys.isBlank()
                ? java.util.List.of() : java.util.List.of(mediaFrameKeys.split("\\n"));
    }

    public void startAnalysis() {
        analysisStatus = AnalysisStatus.ANALYZING;
        analysisError = null;
    }

    public void completeAnalysis(PageMetadata metadata) {
        title = truncate(metadata.title(), 500);
        description = metadata.description();
        thumbnailUrl = truncate(metadata.imageUrl(), 2048);
        extractedPlaceName = truncate(metadata.placeName(), 200);
        extractedCategory = truncate(metadata.category(), 100);
        extractedAddress = truncate(metadata.address(), 500);
        extractedLatitude = metadata.latitude();
        extractedLongitude = metadata.longitude();
        analysisStatus = AnalysisStatus.COMPLETED;
        analysisError = null;
    }

    public void failAnalysis(String error) {
        analysisStatus = AnalysisStatus.FAILED;
        analysisError = error;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
