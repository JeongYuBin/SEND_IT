package com.sendit.share;

import com.sendit.share.ShareDtos.CreateShareRequest;
import com.sendit.share.ShareDtos.ShareAcceptedResponse;
import com.sendit.share.ShareDtos.ShareDetailResponse;
import com.sendit.user.User;
import com.sendit.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShareService {

    private final UserRepository userRepository;
    private final SharedContentRepository sharedContentRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final UrlNormalizer urlNormalizer;

    public ShareService(
            UserRepository userRepository,
            SharedContentRepository sharedContentRepository,
            AnalysisJobRepository analysisJobRepository,
            UrlNormalizer urlNormalizer
    ) {
        this.userRepository = userRepository;
        this.sharedContentRepository = sharedContentRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.urlNormalizer = urlNormalizer;
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

    public ShareAcceptedResponse reanalyze(String email, Long shareId) {
        SharedContent content = findOwnedContent(email, shareId);
        content.queueForAnalysis();
        analysisJobRepository.save(new AnalysisJob(content));
        return accepted(content, false, "콘텐츠 재분석을 요청했습니다.");
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
                content.getCreatedAt()
        );
    }
}
