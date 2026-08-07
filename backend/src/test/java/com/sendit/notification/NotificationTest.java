package com.sendit.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationTest {
    @Test
    void marksUnreadNotificationAsRead() {
        Notification notification = new Notification(null,
                NotificationType.ANALYSIS_COMPLETED, "완료", "분석 완료", "/shares/1");

        assertThat(notification.getReadAt()).isNull();
        notification.markRead();
        assertThat(notification.getReadAt()).isNotNull();
    }
}
