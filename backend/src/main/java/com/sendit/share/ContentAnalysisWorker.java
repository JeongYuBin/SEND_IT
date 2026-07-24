package com.sendit.share;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContentAnalysisWorker {

    private final AnalysisJobService analysisJobService;
    private final SafePageFetcher safePageFetcher;
    private final PageMetadataParser pageMetadataParser;

    public ContentAnalysisWorker(
            AnalysisJobService analysisJobService,
            SafePageFetcher safePageFetcher,
            PageMetadataParser pageMetadataParser
    ) {
        this.analysisJobService = analysisJobService;
        this.safePageFetcher = safePageFetcher;
        this.pageMetadataParser = pageMetadataParser;
    }

    @Scheduled(fixedDelayString = "${app.analysis.poll-delay-ms}")
    public void processNext() {
        analysisJobService.claimNext().ifPresent(job -> {
            try {
                var page = safePageFetcher.fetch(job.url());
                PageMetadata metadata = pageMetadataParser.parse(page.html(), page.finalUrl());
                analysisJobService.complete(job.jobId(), metadata);
            } catch (RuntimeException exception) {
                analysisJobService.fail(job.jobId(), exception.getMessage());
            }
        });
    }
}

