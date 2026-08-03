package com.sendit.share;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final int maxRetries;

    public AnalysisJobService(
            AnalysisJobRepository analysisJobRepository,
            @Value("${app.analysis.max-retries}") int maxRetries
    ) {
        this.analysisJobRepository = analysisJobRepository;
        this.maxRetries = maxRetries;
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
                job.getSharedContent().getSharedText()
        ));
    }

    @Transactional
    public void complete(Long jobId, PageMetadata metadata) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("분석 작업을 찾을 수 없습니다."));
        job.complete(Instant.now(), metadata);
    }

    @Transactional
    public void retryOrFail(Long jobId, String error) {
        analysisJobRepository.findById(jobId)
                .ifPresent(job -> job.retryOrFail(Instant.now(), truncate(error), maxRetries));
    }

    private String truncate(String error) {
        if (error == null) {
            return "알 수 없는 분석 오류입니다.";
        }
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }

    public record ClaimedJob(Long jobId, Long sharedContentId, String url, String sharedText) {
    }
}
