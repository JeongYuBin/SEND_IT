package com.sendit.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.sendit.share.AnalysisStatus;
import com.sendit.share.SharedContent;
import com.sendit.user.User;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {
    @Test
    void deletesOnlyReadNotificationsOwnedByUser() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        when(notifications.deleteByUserEmailAndReadAtIsNotNull("user@example.com"))
                .thenReturn(3L);
        NotificationService service = new NotificationService(notifications);

        long deletedCount = service.deleteRead("user@example.com");

        assertThat(deletedCount).isEqualTo(3);
        verify(notifications).deleteByUserEmailAndReadAtIsNotNull("user@example.com");
    }

    @Test
    void replacesPreviousAnalysisNotificationWithLatestResult() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        SharedContent share = mock(SharedContent.class);
        User user = mock(User.class);
        when(share.getId()).thenReturn(42L);
        when(share.getUser()).thenReturn(user);
        when(share.getAnalysisStatus()).thenReturn(AnalysisStatus.COMPLETED);
        when(share.getExtractedPlaceName()).thenReturn("테스트 장소");
        when(user.getEmail()).thenReturn("user@example.com");
        NotificationService service = new NotificationService(notifications);

        service.notifyAnalysisResult(share);

        verify(notifications).deleteByUserEmailAndTargetUrl(
                "user@example.com", "/shares/42");
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertThat(captor.getValue().getUniqueKey())
                .isEqualTo("analysis:42:COMPLETED");
    }

    @Test
    void doesNotCreateDuplicateAnalysisNotification() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        SharedContent share = mock(SharedContent.class);
        when(share.getId()).thenReturn(42L);
        when(share.getAnalysisStatus()).thenReturn(AnalysisStatus.COMPLETED);
        when(notifications.existsByUniqueKey("analysis:42:COMPLETED")).thenReturn(true);
        NotificationService service = new NotificationService(notifications);

        service.notifyAnalysisResult(share);

        verify(notifications, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
