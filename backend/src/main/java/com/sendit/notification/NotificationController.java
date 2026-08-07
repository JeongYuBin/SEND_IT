package com.sendit.notification;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    List<NotificationDtos.Response> list(Principal principal) {
        return service.list(principal.getName());
    }

    @GetMapping("/unread-count")
    NotificationDtos.UnreadCount unreadCount(Principal principal) {
        return service.unreadCount(principal.getName());
    }

    @PatchMapping("/{id}/read")
    NotificationDtos.Response markRead(Principal principal, @PathVariable Long id) {
        return service.markRead(principal.getName(), id);
    }

    @PatchMapping("/read-all")
    void markAllRead(Principal principal) {
        service.markAllRead(principal.getName());
    }
}
