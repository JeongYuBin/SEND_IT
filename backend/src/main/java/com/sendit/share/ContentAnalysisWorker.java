package com.sendit.share;

import com.sendit.tourism.TourApiClient;
import com.sendit.place.SavedPlaceService;
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
    private final KakaoPlaceSearchClient kakaoPlaceSearchClient;
    private final SavedPlaceService savedPlaceService;
    private final AutomaticMediaDownloader automaticMediaDownloader;
    private final VideoMediaProcessor videoMediaProcessor;
    private final FrameOcrExtractor frameOcrExtractor;
    private final AudioTranscriber audioTranscriber;

    public ContentAnalysisWorker(
            AnalysisJobService analysisJobService,
            SafePageFetcher safePageFetcher,
            PageMetadataParser pageMetadataParser,
            PlatformMetadataAnalyzer platformMetadataAnalyzer,
            SharedTextMetadataParser sharedTextMetadataParser,
            VisitKoreaMetadataClient visitKoreaMetadataClient,
            TourApiClient tourApiClient,
            KakaoPlaceSearchClient kakaoPlaceSearchClient,
            SavedPlaceService savedPlaceService,
            AutomaticMediaDownloader automaticMediaDownloader,
            VideoMediaProcessor videoMediaProcessor,
            FrameOcrExtractor frameOcrExtractor,
            AudioTranscriber audioTranscriber
    ) {
        this.analysisJobService = analysisJobService;
        this.safePageFetcher = safePageFetcher;
        this.pageMetadataParser = pageMetadataParser;
        this.platformMetadataAnalyzer = platformMetadataAnalyzer;
        this.sharedTextMetadataParser = sharedTextMetadataParser;
        this.visitKoreaMetadataClient = visitKoreaMetadataClient;
        this.tourApiClient = tourApiClient;
        this.kakaoPlaceSearchClient = kakaoPlaceSearchClient;
        this.savedPlaceService = savedPlaceService;
        this.automaticMediaDownloader = automaticMediaDownloader;
        this.videoMediaProcessor = videoMediaProcessor;
        this.frameOcrExtractor = frameOcrExtractor;
        this.audioTranscriber = audioTranscriber;
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
                metadata = kakaoPlaceSearchClient.enrich(metadata);
                metadata = tourApiClient.enrich(metadata);
                boolean needsConfirmation = metadata.placeName() == null || metadata.placeName().isBlank();
                String mediaStorageKey = job.mediaStorageKey();
                java.util.List<String> frameKeys = job.mediaFrameKeys();
                String ocrText = job.mediaOcrText();
                String audioStorageKey = job.mediaAudioStorageKey();
                String transcript = job.mediaTranscript();
                if (needsConfirmation
                        && mediaStorageKey == null
                        && automaticMediaDownloader.supports(job.url())) {
                    try {
                        StoredMedia media = automaticMediaDownloader.download(job.url());
                        analysisJobService.attachMedia(job.jobId(), media);
                        mediaStorageKey = media.storageKey();
                    } catch (RuntimeException ignored) {
                        // 캡션 분석 결과는 유지하고 영상 확보 실패는 재분석 화면에서 다시 시도한다.
                    }
                }
                if (needsConfirmation && mediaStorageKey != null && !job.mediaProcessed()) {
                    try {
                        MediaProcessingResult processingResult = videoMediaProcessor.process(mediaStorageKey);
                        analysisJobService.attachMediaProcessingResult(job.jobId(), processingResult);
                        frameKeys = processingResult.frameStorageKeys();
                        audioStorageKey = processingResult.audioStorageKey();
                    } catch (RuntimeException ignored) {
                        // 영상 원본은 유지하고 프레임·음원 추출은 재분석에서 다시 시도한다.
                    }
                }
                if (needsConfirmation && !frameKeys.isEmpty()
                        && (ocrText == null || ocrText.isBlank())) {
                    try {
                        ocrText = frameOcrExtractor.extract(frameKeys);
                        if (ocrText != null && !ocrText.isBlank()) {
                            analysisJobService.attachMediaOcrText(job.jobId(), ocrText);
                            PageMetadata fromFrames = sharedTextMetadataParser.parse(ocrText);
                            metadata = sharedTextMetadataParser.merge(metadata, fromFrames);
                            metadata = kakaoPlaceSearchClient.enrich(metadata);
                            metadata = tourApiClient.enrich(metadata);
                            needsConfirmation = metadata.placeName() == null
                                    || metadata.placeName().isBlank();
                        }
                    } catch (RuntimeException ignored) {
                        // OCR 실패 시 프레임을 유지하고 재분석에서 다시 시도한다.
                    }
                }
                if (needsConfirmation && audioStorageKey != null
                        && (transcript == null || transcript.isBlank())) {
                    try {
                        transcript = audioTranscriber.transcribe(audioStorageKey);
                        if (transcript != null && !transcript.isBlank()) {
                            analysisJobService.attachMediaTranscript(job.jobId(), transcript);
                            PageMetadata fromSpeech = sharedTextMetadataParser.parse(transcript);
                            metadata = sharedTextMetadataParser.merge(metadata, fromSpeech);
                            metadata = kakaoPlaceSearchClient.enrich(metadata);
                            metadata = tourApiClient.enrich(metadata);
                            needsConfirmation = metadata.placeName() == null
                                    || metadata.placeName().isBlank();
                        }
                    } catch (RuntimeException ignored) {
                        // STT 실패 시 기존 메타데이터와 OCR 결과로 분석을 마무리한다.
                    }
                }
                analysisJobService.complete(job.jobId(), metadata, needsConfirmation);
                try {
                    savedPlaceService.autoSaveAnalyzedShare(job.sharedContentId());
                } catch (RuntimeException ignored) {
                    // 분석 결과는 유지하고 자동 저장 실패 시 결과 화면에서 직접 저장할 수 있게 한다.
                }
            } catch (RuntimeException exception) {
                analysisJobService.retryOrFail(job.jobId(), exception.getMessage());
            }
        });
    }
}
