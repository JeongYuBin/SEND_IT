package com.sendit.share;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContentAnalysisWorker {

    private final AnalysisJobService analysisJobService;
    private final SafePageFetcher safePageFetcher;
    private final PageMetadataParser pageMetadataParser;
    private final VisitKoreaMetadataClient visitKoreaMetadataClient;

    public ContentAnalysisWorker(
            AnalysisJobService analysisJobService,
            SafePageFetcher safePageFetcher,
            PageMetadataParser pageMetadataParser,
            VisitKoreaMetadataClient visitKoreaMetadataClient
    ) {
        this.analysisJobService = analysisJobService;
        this.safePageFetcher = safePageFetcher;
        this.pageMetadataParser = pageMetadataParser;
        this.visitKoreaMetadataClient = visitKoreaMetadataClient;
    }

    @Scheduled(fixedDelayString = "${app.analysis.poll-delay-ms}")
    public void processNext() {
        analysisJobService.claimNext().ifPresent(job -> {
            try {
                var page = safePageFetcher.fetch(job.url());
                PageMetadata metadata = pageMetadataParser.parse(page.html(), page.finalUrl());
                metadata = visitKoreaMetadataClient.enrich(page.finalUrl(), metadata);
                analysisJobService.complete(job.jobId(), metadata);
            } catch (RuntimeException exception) {
                analysisJobService.retryOrFail(job.jobId(), exception.getMessage());
            }
        });
    }
}
