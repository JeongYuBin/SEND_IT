package com.sendit.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
