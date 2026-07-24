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

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 30)
    private AnalysisStatus analysisStatus;

    @Column(name = "analysis_error", columnDefinition = "text")
    private String analysisError;

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

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public String getAnalysisError() {
        return analysisError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void queueForAnalysis() {
        analysisStatus = AnalysisStatus.PENDING;
        analysisError = null;
    }
}

