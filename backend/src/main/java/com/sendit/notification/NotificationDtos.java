package com.sendit.notification;

import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record Response(Long id, NotificationType type, String title, String message,
                           String targetUrl, boolean read, Instant createdAt) {}
    public record UnreadCount(long count) {}
}
