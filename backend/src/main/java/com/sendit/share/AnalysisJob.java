package com.sendit.share;

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

@Entity
@Table(name = "analysis_jobs")
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_content_id", nullable = false)
    private SharedContent sharedContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AnalysisJob() {
    }

    public AnalysisJob(SharedContent sharedContent) {
        this.sharedContent = sharedContent;
        this.status = JobStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public SharedContent getSharedContent() {
        return sharedContent;
    }

    public void start(Instant now) {
        status = JobStatus.PROCESSING;
        startedAt = now;
        sharedContent.startAnalysis();
    }

    public void complete(Instant now, PageMetadata metadata) {
        status = JobStatus.COMPLETED;
        completedAt = now;
        errorMessage = null;
        sharedContent.completeAnalysis(metadata);
    }

    public void fail(Instant now, String error) {
        status = JobStatus.FAILED;
        completedAt = now;
        errorMessage = error;
        sharedContent.failAnalysis(error);
    }
}
