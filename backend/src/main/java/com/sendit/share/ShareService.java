package com.sendit.share;

import com.sendit.share.ShareDtos.CreateShareRequest;
import com.sendit.share.ShareDtos.ShareAcceptedResponse;
import com.sendit.share.ShareDtos.ShareDetailResponse;
import com.sendit.user.User;
import com.sendit.user.UserRepository;
import com.sendit.notification.NotificationService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@Transactional
public class ShareService {

    private final UserRepository userRepository;
    private final SharedContentRepository sharedContentRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final UrlNormalizer urlNormalizer;
    private final MediaStorageCleaner mediaStorageCleaner;
    private final NotificationService notificationService;
    private final SharedContentPlaceRepository extractedPlaces;

    public ShareService(
            UserRepository userRepository,
            SharedContentRepository sharedContentRepository,
            AnalysisJobRepository analysisJobRepository,
            UrlNormalizer urlNormalizer,
            MediaStorageCleaner mediaStorageCleaner,
            NotificationService notificationService,
            SharedContentPlaceRepository extractedPlaces
    ) {
        this.userRepository = userRepository;
        this.sharedContentRepository = sharedContentRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.urlNormalizer = urlNormalizer;
        this.mediaStorageCleaner = mediaStorageCleaner;
        this.notificationService = notificationService;
        this.extractedPlaces = extractedPlaces;
    }

    public ShareAcceptedResponse create(String email, CreateShareRequest request) {
        User user = findUser(email);
        String normalizedUrl = urlNormalizer.normalize(request.url());
        var existing = sharedContentRepository
                .findByUserIdAndNormalizedUrl(user.getId(), normalizedUrl);
        if (existing.isPresent()) {
            SharedContent content = existing.get();
            return accepted(content, true, "이미 저장된 콘텐츠입니다.");
        }

        SourceType sourceType = request.sourceType() != null
                ? request.sourceType()
                : urlNormalizer.detectSource(normalizedUrl);
        SharedContent content = sharedContentRepository.save(new SharedContent(
                user,
                request.url().trim(),
                normalizedUrl,
                sourceType,
                request.sharedText()
        ));
        analysisJobRepository.save(new AnalysisJob(content));
        return accepted(content, false, "콘텐츠 분석을 요청했습니다.");
    }

    @Transactional(readOnly = true)
    public ShareDetailResponse get(String email, Long shareId) {
        return toDetail(findOwnedContent(email, shareId));
    }

    @Transactional(readOnly = true)
    public List<ShareDetailResponse> list(String email) {
        return sharedContentRepository.findAllByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShareDtos.SharePageResponse page(String email, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        var result = sharedContentRepository.findByUserEmail(
                email,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new ShareDtos.SharePageResponse(
                result.getContent().stream().map(this::toDetail).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isLast());
    }

    public ShareAcceptedResponse reanalyze(String email, Long shareId) {
        SharedContent content = findOwnedContent(email, shareId);
        if (analysisJobRepository.existsBySharedContentIdAndStatusIn(
                shareId, List.of(JobStatus.PENDING, JobStatus.PROCESSING))) {
            return accepted(content, true, "이미 분석이 진행 중입니다.");
        }
        content.queueForAnalysis();
        analysisJobRepository.save(new AnalysisJob(content));
        return accepted(content, false, "콘텐츠 재분석을 요청했습니다.");
    }

    public void delete(String email, Long shareId) {
        SharedContent content = findOwnedContent(email, shareId);
        if (isAnalysisActive(content)) {
            throw new IllegalArgumentException("분석 중인 콘텐츠는 완료된 뒤 삭제해 주세요.");
        }
        deleteContent(email, content);
        sharedContentRepository.flush();
    }

    public void deleteAll(String email) {
        List<SharedContent> contents = sharedContentRepository.findAllByUserEmailOrderByCreatedAtDesc(email);
        if (contents.stream().anyMatch(this::isAnalysisActive)) {
            throw new IllegalArgumentException("분석 중인 콘텐츠가 있습니다. 분석이 끝난 뒤 다시 시도해 주세요.");
        }
        contents.forEach(content -> deleteContent(email, content));
        sharedContentRepository.flush();
    }

    private boolean isAnalysisActive(SharedContent content) {
        return content.getAnalysisStatus() == AnalysisStatus.PENDING
                || content.getAnalysisStatus() == AnalysisStatus.ANALYZING;
    }

    private void deleteContent(String email, SharedContent content) {
        List<String> mediaKeys = new ArrayList<>(content.getMediaFrameKeys());
        mediaKeys.add(content.getMediaStorageKey());
        mediaKeys.add(content.getMediaAudioStorageKey());
        notificationService.deleteForTarget(email, "/shares/" + content.getId());
        sharedContentRepository.delete(content);
        mediaStorageCleaner.deleteAll(mediaKeys);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private SharedContent findOwnedContent(String email, Long id) {
        return sharedContentRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ShareNotFoundException(id));
    }

    private ShareAcceptedResponse accepted(
            SharedContent content,
            boolean duplicate,
            String message
    ) {
        return new ShareAcceptedResponse(
                content.getId(),
                content.getAnalysisStatus(),
                message,
                duplicate
        );
    }

    private ShareDetailResponse toDetail(SharedContent content) {
        return new ShareDetailResponse(
                content.getId(),
                content.getOriginalUrl(),
                content.getSourceType(),
                content.getSharedText(),
                content.getTitle(),
                content.getDescription(),
                content.getThumbnailUrl(),
                content.getAnalysisStatus(),
                content.getAnalysisError(),
                content.getExtractedPlaceName(),
                content.getExtractedCategory(),
                content.getExtractedAddress(),
                content.getExtractedLatitude(),
                content.getExtractedLongitude(),
                content.getMediaOriginalFilename(),
                content.getMediaContentType(),
                content.getMediaSizeBytes(),
                content.getMediaDurationSeconds(),
                content.getMediaFrameCount(),
                content.hasMediaAudio(),
                content.getMediaOcrText(),
                content.getMediaTranscript(),
                extractedPlaces.findBySharedContentIdOrderByDisplayOrder(content.getId()).stream()
                        .map(place -> new ShareDtos.ExtractedPlaceResponse(
                                place.getId(), place.getDisplayOrder(), place.getName(),
                                place.getCategory(), place.getAddress(), place.getLatitude(),
                                place.getLongitude(), place.getImageUrl(), place.getSavedPlaceId()))
                        .toList(),
                content.getCreatedAt()
        );
    }
}
