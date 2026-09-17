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
    private final KakaoPlaceSearchClient kakaoPlaceSearchClient;
    private final AutomaticMediaDownloader automaticMediaDownloader;
    private final VideoMediaProcessor videoMediaProcessor;
    private final FrameOcrExtractor frameOcrExtractor;
    private final AudioTranscriber audioTranscriber;
    private final PlaceVerificationPolicy placeVerificationPolicy;
    private final InstagramCarouselDownloader instagramCarouselDownloader;
    private final MultiPlaceExtractor multiPlaceExtractor;
    private final SharedContentPlaceService sharedContentPlaceService;
    private final CarouselPlaceImageService carouselPlaceImageService;
    private final YouTubeCaptionExtractor youtubeCaptionExtractor;
    private final MediaStorageCleaner mediaStorageCleaner;

    public ContentAnalysisWorker(
            AnalysisJobService analysisJobService,
            SafePageFetcher safePageFetcher,
            PageMetadataParser pageMetadataParser,
            PlatformMetadataAnalyzer platformMetadataAnalyzer,
            SharedTextMetadataParser sharedTextMetadataParser,
            VisitKoreaMetadataClient visitKoreaMetadataClient,
            TourApiClient tourApiClient,
            KakaoPlaceSearchClient kakaoPlaceSearchClient,
            AutomaticMediaDownloader automaticMediaDownloader,
            VideoMediaProcessor videoMediaProcessor,
            FrameOcrExtractor frameOcrExtractor,
            AudioTranscriber audioTranscriber,
            PlaceVerificationPolicy placeVerificationPolicy,
            InstagramCarouselDownloader instagramCarouselDownloader,
            MultiPlaceExtractor multiPlaceExtractor,
            SharedContentPlaceService sharedContentPlaceService,
            CarouselPlaceImageService carouselPlaceImageService,
            YouTubeCaptionExtractor youtubeCaptionExtractor,
            MediaStorageCleaner mediaStorageCleaner
    ) {
        this.analysisJobService = analysisJobService;
        this.safePageFetcher = safePageFetcher;
        this.pageMetadataParser = pageMetadataParser;
        this.platformMetadataAnalyzer = platformMetadataAnalyzer;
        this.sharedTextMetadataParser = sharedTextMetadataParser;
        this.visitKoreaMetadataClient = visitKoreaMetadataClient;
        this.tourApiClient = tourApiClient;
        this.kakaoPlaceSearchClient = kakaoPlaceSearchClient;
        this.automaticMediaDownloader = automaticMediaDownloader;
        this.videoMediaProcessor = videoMediaProcessor;
        this.frameOcrExtractor = frameOcrExtractor;
        this.audioTranscriber = audioTranscriber;
        this.placeVerificationPolicy = placeVerificationPolicy;
        this.instagramCarouselDownloader = instagramCarouselDownloader;
        this.multiPlaceExtractor = multiPlaceExtractor;
        this.sharedContentPlaceService = sharedContentPlaceService;
        this.carouselPlaceImageService = carouselPlaceImageService;
        this.youtubeCaptionExtractor = youtubeCaptionExtractor;
        this.mediaStorageCleaner = mediaStorageCleaner;
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
                    if (!sharedTextMetadataParser.hasContent(shared)
                            && !automaticMediaDownloader.supports(job.url())
                            && !instagramCarouselDownloader.supports(job.url())) throw fetchFailure;
                    metadata = shared;
                }
                PageMetadata discoveredFromPageText = sharedTextMetadataParser.parse(
                        String.join("\n",
                                metadata.title() == null ? "" : metadata.title(),
                                metadata.description() == null ? "" : metadata.description()));
                metadata = sharedTextMetadataParser.merge(metadata, discoveredFromPageText);
                metadata = sharedTextMetadataParser.merge(metadata, shared);
                var captionPlaces = multiPlaceExtractor.extractDescription(metadata.description(), metadata);
                metadata = visitKoreaMetadataClient.enrich(job.url(), metadata);
                metadata = kakaoPlaceSearchClient.enrich(metadata);
                metadata = tourApiClient.enrich(metadata);
                boolean needsConfirmation = !placeVerificationPolicy.isVerified(metadata);
                boolean completeCaption = !captionPlaces.isEmpty()
                        && captionPlaces.size() == multiPlaceExtractor.descriptionPlaceCount(metadata.description());
                boolean analyzeMedia = !completeCaption && (needsConfirmation || shouldDeepAnalyze(job.url(), metadata));
                String mediaStorageKey = job.mediaStorageKey();
                java.util.List<String> frameKeys = job.mediaFrameKeys();
                String ocrText = job.mediaOcrText();
                String audioStorageKey = job.mediaAudioStorageKey();
                String transcript = job.mediaTranscript();
                if (analyzeMedia && (transcript == null || transcript.isBlank())) {
                    transcript = youtubeCaptionExtractor.extract(job.url());
                    if (transcript != null && !transcript.isBlank()) {
                        analysisJobService.attachMediaTranscript(job.jobId(), transcript);
                    }
                }
                if (analyzeMedia && frameKeys.isEmpty()
                        && instagramCarouselDownloader.supports(job.url())) {
                    try {
                        MediaProcessingResult carousel = instagramCarouselDownloader.downloadFrames(job.url());
                        if (!carousel.frameStorageKeys().isEmpty()) {
                            analysisJobService.attachMediaProcessingResult(job.jobId(), carousel);
                            frameKeys = carousel.frameStorageKeys();
                        }
                    } catch (RuntimeException ignored) {
                        // A missing carousel must not prevent reel/audio analysis or caption extraction.
                    }
                }
                if (analyzeMedia
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
                if (analyzeMedia && mediaStorageKey != null && !job.mediaProcessed()) {
                    try {
                        MediaProcessingResult processingResult = videoMediaProcessor.process(mediaStorageKey);
                        analysisJobService.attachMediaProcessingResult(job.jobId(), processingResult);
                        frameKeys = processingResult.frameStorageKeys();
                        audioStorageKey = processingResult.audioStorageKey();
                    } catch (RuntimeException ignored) {
                        // 영상 원본은 유지하고 프레임·음원 추출은 재분석에서 다시 시도한다.
                    }
                }
                if (analyzeMedia && !frameKeys.isEmpty()
                        && (ocrText == null || ocrText.isBlank())) {
                    try {
                        ocrText = frameOcrExtractor.extract(frameKeys);
                        if (ocrText != null && !ocrText.isBlank()) {
                            analysisJobService.attachMediaOcrText(job.jobId(), ocrText);
                        }
                    } catch (RuntimeException ignored) {
                        // OCR 실패 시 프레임을 유지하고 재분석에서 다시 시도한다.
                    }
                }
                if (analyzeMedia && audioStorageKey != null
                        && (transcript == null || transcript.isBlank())) {
                    try {
                        transcript = audioTranscriber.transcribe(audioStorageKey);
                        if (transcript != null && !transcript.isBlank()) {
                            analysisJobService.attachMediaTranscript(job.jobId(), transcript);
                        }
                    } catch (RuntimeException ignored) {
                        // STT 실패 시 기존 메타데이터와 OCR 결과로 분석을 마무리한다.
                    }
                }
                // Reuse cached OCR/transcripts on reanalysis, including YouTube captions without audio.
                metadata = sharedTextMetadataParser.merge(metadata, sharedTextMetadataParser.parse(ocrText));
                metadata = sharedTextMetadataParser.merge(metadata, sharedTextMetadataParser.parse(transcript));
                metadata = kakaoPlaceSearchClient.enrich(metadata);
                metadata = tourApiClient.enrich(metadata);
                needsConfirmation = !placeVerificationPolicy.isVerified(metadata);
                java.util.List<PageMetadata> extractedPlaces = new java.util.ArrayList<>(
                        multiPlaceExtractor.extract(ocrText, metadata));
                java.util.List<PageMetadata> textPlaces = new java.util.ArrayList<>(
                        captionPlaces);
                textPlaces.addAll(multiPlaceExtractor.extractCaption(transcript, metadata));
                // A structured list is authoritative; do not append a truncated headline landmark.
                if (textPlaces.isEmpty() && placeVerificationPolicy.isVerified(metadata)) textPlaces.add(metadata);
                for (PageMetadata place : textPlaces) {
                    boolean duplicate = extractedPlaces.stream().anyMatch(existing ->
                            normalizePlace(existing.placeName()).equals(normalizePlace(place.placeName()))
                                    && normalizePlace(existing.address()).equals(normalizePlace(place.address())));
                    if (!duplicate) extractedPlaces.add(place);
                }
                extractedPlaces = extractedPlaces.stream()
                        .filter(placeVerificationPolicy::isVerified)
                        .map(this::enrichPlaceImage)
                        .toList();
                // Instagram 이미지 게시물은 각 슬라이드가 장소별 원본 이미지다.
                // 영상 대표 프레임이나 YouTube 썸네일은 개별 업체 사진으로 사용하지 않는다.
                if (instagramCarouselDownloader.supports(job.url())) {
                    extractedPlaces = carouselPlaceImageService.attach(extractedPlaces, frameKeys);
                }
                if (!extractedPlaces.isEmpty()) {
                    PageMetadata first = extractedPlaces.getFirst();
                    metadata = new PageMetadata(
                            metadata.title(), metadata.description(), metadata.imageUrl(),
                            first.placeName(), first.category(), first.address(),
                            first.latitude(), first.longitude());
                    needsConfirmation = false;
                }
                // Publish candidates before COMPLETED becomes visible to the share save sheet.
                sharedContentPlaceService.replace(job.sharedContentId(), extractedPlaces);
                analysisJobService.complete(job.jobId(), metadata, needsConfirmation);
                mediaStorageCleaner.deleteTransient(mediaStorageKey, audioStorageKey);
                mediaStorageCleaner.deleteAll(frameKeys);
            } catch (RuntimeException exception) {
                analysisJobService.retryOrFail(job.jobId(), exception.getMessage());
            }
        });
    }

    private String normalizePlace(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.KOREAN)
                .replaceAll("[^0-9a-z가-힣]", "");
    }

    private PageMetadata enrichPlaceImage(PageMetadata place) {
        PageMetadata withoutSourceThumbnail = new PageMetadata(
                place.title(), place.description(), null,
                place.placeName(), place.category(), place.address(),
                place.latitude(), place.longitude());
        return tourApiClient.enrich(withoutSourceThumbnail);
    }

    private boolean shouldDeepAnalyze(String url, PageMetadata metadata) {
        if (url == null || !automaticMediaDownloader.supports(url)) {
            return false;
        }
        String text = String.join(" ", metadata.title() == null ? "" : metadata.title(),
                metadata.description() == null ? "" : metadata.description());
        return text.matches("(?s).*(맛집|먹방|카페|투어|총정리|또간집|곳|BEST|best).*" );
    }
}
