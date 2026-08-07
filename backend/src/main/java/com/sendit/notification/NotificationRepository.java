package com.sendit.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderByCreatedAtDesc(String email);
    Optional<Notification> findByIdAndUserEmail(Long id, String email);
    long countByUserEmailAndReadAtIsNull(String email);
    void deleteByUserEmailAndTargetUrl(String email, String targetUrl);
}
