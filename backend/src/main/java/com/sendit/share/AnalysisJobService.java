package com.sendit.share;

import com.sendit.notification.NotificationService;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final NotificationService notificationService;
    private final int maxRetries;
    private final long staleTimeoutSeconds;

    public AnalysisJobService(
            AnalysisJobRepository analysisJobRepository,
            NotificationService notificationService,
            @Value("${app.analysis.max-retries}") int maxRetries,
            @Value("${app.analysis.stale-timeout-seconds:1800}") long staleTimeoutSeconds
    ) {
        this.analysisJobRepository = analysisJobRepository;
        this.notificationService = notificationService;
        this.maxRetries = maxRetries;
        this.staleTimeoutSeconds = Math.max(60, staleTimeoutSeconds);
    }

    @Transactional
    public Optional<ClaimedJob> claimNext() {
        var jobs = analysisJobRepository.findByStatusOrderByCreatedAtAsc(
                JobStatus.PENDING,
                PageRequest.of(0, 1)
        );
        if (jobs.isEmpty()) {
            return Optional.empty();
        }
        AnalysisJob job = jobs.getFirst();
        job.start(Instant.now());
        return Optional.of(new ClaimedJob(
                job.getId(),
                job.getSharedContent().getId(),
                job.getSharedContent().getNormalizedUrl(),
                job.getSharedContent().getSharedText(),
                job.getSharedContent().getMediaStorageKey(),
                job.getSharedContent().hasProcessedMedia(),
                job.getSharedContent().getMediaFrameKeys(),
                job.getSharedContent().getMediaOcrText(),
                job.getSharedContent().getMediaAudioStorageKey(),
                job.getSharedContent().getMediaTranscript()
        ));
    }

    @Transactional
    public void complete(Long jobId, PageMetadata metadata, boolean needsConfirmation) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("분석 작업을 찾을 수 없습니다."));
        job.complete(Instant.now(), metadata);
        if (needsConfirmation) job.getSharedContent().requireConfirmation();
        notificationService.notifyAnalysisResult(job.getSharedContent());
    }

    @Transactional
    public void attachMedia(Long jobId, StoredMedia media) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("분석 작업을 찾을 수 없습니다."));
        job.getSharedContent().attachMedia(media);
    }

    @Transactional
    public void attachMediaProcessingResult(Long jobId, MediaProcessingResult result) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("분석 작업을 찾을 수 없습니다."));
        job.getSharedContent().attachMediaProcessingResult(result);
    }

    @Transactional
    public void attachMediaOcrText(Long jobId, String ocrText) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("분석 작업을 찾을 수 없습니다."));
        job.getSharedContent().attachMediaOcrText(ocrText);
    }

    @Transactional
    public void attachMediaTranscript(Long jobId, String transcript) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("분석 작업을 찾을 수 없습니다."));
        job.getSharedContent().attachMediaTranscript(transcript);
    }

    @Transactional
    public void retryOrFail(Long jobId, String error) {
        analysisJobRepository.findById(jobId).ifPresent(job -> {
            job.retryOrFail(Instant.now(), truncate(error), maxRetries);
            if (job.getStatus() == JobStatus.FAILED) {
                notificationService.notifyAnalysisResult(job.getSharedContent());
            }
        });
    }

    @Scheduled(fixedDelayString = "${app.analysis.recovery-delay-ms:60000}")
    @Transactional
    public void recoverStaleJobs() {
        Instant cutoff = Instant.now().minusSeconds(staleTimeoutSeconds);
        var jobs = analysisJobRepository
                .findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                        JobStatus.PROCESSING, cutoff, PageRequest.of(0, 50));
        for (AnalysisJob job : jobs) {
            job.retryOrFail(Instant.now(), "분석 서버 중단으로 작업을 자동 복구했습니다.", maxRetries);
            if (job.getStatus() == JobStatus.FAILED) {
                notificationService.notifyAnalysisResult(job.getSharedContent());
            }
        }
    }

    private String truncate(String error) {
        if (error == null) {
            return "알 수 없는 분석 오류입니다.";
        }
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }

    public record ClaimedJob(
            Long jobId,
            Long sharedContentId,
            String url,
            String sharedText,
            String mediaStorageKey,
            boolean mediaProcessed,
            java.util.List<String> mediaFrameKeys,
            String mediaOcrText,
            String mediaAudioStorageKey,
            String mediaTranscript
    ) {
    }
}
