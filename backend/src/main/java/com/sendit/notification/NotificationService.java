package com.sendit.notification;

import com.sendit.collection.ResourceNotFoundException;
import com.sendit.share.AnalysisStatus;
import com.sendit.share.SharedContent;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    public void notifyAnalysisResult(SharedContent share) {
        NotificationType type;
        String title;
        String message;
        if (share.getAnalysisStatus() == AnalysisStatus.COMPLETED) {
            type = NotificationType.ANALYSIS_COMPLETED;
            title = "장소 분석이 완료되었습니다";
            message = displayName(share) + " 정보를 확인할 수 있습니다.";
        } else if (share.getAnalysisStatus() == AnalysisStatus.NEEDS_CONFIRMATION) {
            type = NotificationType.ANALYSIS_NEEDS_CONFIRMATION;
            title = "장소 정보를 확인해 주세요";
            message = "영상에서 찾은 장소의 주소 또는 위치를 확인해야 합니다.";
        } else if (share.getAnalysisStatus() == AnalysisStatus.FAILED) {
            type = NotificationType.ANALYSIS_FAILED;
            title = "콘텐츠 분석을 완료하지 못했습니다";
            message = "원본 링크를 확인하거나 장소 정보를 직접 입력해 주세요.";
        } else {
            return;
        }
        notifications.save(new Notification(share.getUser(), type, title, message,
                "/shares/" + share.getId()));
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.Response> list(String email) {
        return notifications.findByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public NotificationDtos.UnreadCount unreadCount(String email) {
        return new NotificationDtos.UnreadCount(
                notifications.countByUserEmailAndReadAtIsNull(email));
    }

    public NotificationDtos.Response markRead(String email, Long id) {
        Notification notification = notifications.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다."));
        notification.markRead();
        return response(notification);
    }

    public void markAllRead(String email) {
        notifications.findByUserEmailOrderByCreatedAtDesc(email).stream()
                .filter(notification -> notification.getReadAt() == null)
                .forEach(Notification::markRead);
    }

    public void deleteForTarget(String email, String targetUrl) {
        notifications.deleteByUserEmailAndTargetUrl(email, targetUrl);
    }

    private String displayName(SharedContent share) {
        if (share.getExtractedPlaceName() != null && !share.getExtractedPlaceName().isBlank()) {
            return share.getExtractedPlaceName();
        }
        return "공유한 콘텐츠의 장소";
    }

    private NotificationDtos.Response response(Notification notification) {
        return new NotificationDtos.Response(
                notification.getId(), notification.getType(), notification.getTitle(),
                notification.getMessage(), notification.getTargetUrl(),
                notification.getReadAt() != null, notification.getCreatedAt());
    }
}
