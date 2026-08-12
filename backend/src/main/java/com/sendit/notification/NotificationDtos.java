package com.sendit.notification;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record Response(Long id, NotificationType type, String title, String message,
                           String targetUrl, boolean read, Instant createdAt) {}
    public record UnreadCount(long count) {}
    public record DeleteResult(long deletedCount) {}
    public record PageResponse(List<Response> content, int page, int size,
                               long totalElements, int totalPages, boolean last) {}
}
