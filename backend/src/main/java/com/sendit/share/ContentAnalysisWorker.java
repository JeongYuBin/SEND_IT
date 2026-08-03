package com.sendit.share;

import com.sendit.tourism.TourApiClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContentAnalysisWorker {

    private final AnalysisJobService analysisJobService;
    private final SafePageFetcher safePageFetcher;
    private final PageMetadataParser pageMetadataParser;
    private final PlatformMetadataAnalyzer platformMetadataAnalyzer;
    private final SharedTextMetadataParser sharedTextMetadataParser;
    private final VisitKoreaMetadataClient visitKoreaMetadataClient;
    private final TourApiClient tourApiClient;

    public ContentAnalysisWorker(
            AnalysisJobService analysisJobService,
            SafePageFetcher safePageFetcher,
            PageMetadataParser pageMetadataParser,
            PlatformMetadataAnalyzer platformMetadataAnalyzer,
            SharedTextMetadataParser sharedTextMetadataParser,
            VisitKoreaMetadataClient visitKoreaMetadataClient,
            TourApiClient tourApiClient
    ) {
        this.analysisJobService = analysisJobService;
        this.safePageFetcher = safePageFetcher;
        this.pageMetadataParser = pageMetadataParser;
        this.platformMetadataAnalyzer = platformMetadataAnalyzer;
        this.sharedTextMetadataParser = sharedTextMetadataParser;
        this.visitKoreaMetadataClient = visitKoreaMetadataClient;
        this.tourApiClient = tourApiClient;
    }

    @Scheduled(fixedDelayString = "${app.analysis.poll-delay-ms}")
    public void processNext() {
        analysisJobService.claimNext().ifPresent(job -> {
            try {
                PageMetadata shared = sharedTextMetadataParser.parse(job.sharedText());
                PageMetadata metadata;
                try {
                    metadata = platformMetadataAnalyzer.analyze(job.url())
                            .orElseGet(() -> {
                                var page = safePageFetcher.fetch(job.url());
                                return pageMetadataParser.parse(page.html(), page.finalUrl());
                            });
                } catch (RuntimeException fetchFailure) {
                    if (!sharedTextMetadataParser.hasContent(shared)) throw fetchFailure;
                    metadata = shared;
                }
                PageMetadata discoveredFromPageText = sharedTextMetadataParser.parse(
                        String.join("\n",
                                metadata.title() == null ? "" : metadata.title(),
                                metadata.description() == null ? "" : metadata.description()));
                metadata = sharedTextMetadataParser.merge(metadata, discoveredFromPageText);
                metadata = sharedTextMetadataParser.merge(metadata, shared);
                metadata = visitKoreaMetadataClient.enrich(job.url(), metadata);
                metadata = tourApiClient.enrich(metadata);
                analysisJobService.complete(job.jobId(), metadata);
            } catch (RuntimeException exception) {
                analysisJobService.retryOrFail(job.jobId(), exception.getMessage());
            }
        });
    }
}
