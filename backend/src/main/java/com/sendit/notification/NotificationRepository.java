package com.sendit.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderByCreatedAtDesc(String email);
    Page<Notification> findByUserEmail(String email, Pageable pageable);
    Optional<Notification> findByIdAndUserEmail(Long id, String email);
    long countByUserEmailAndReadAtIsNull(String email);
    boolean existsByUniqueKey(String uniqueKey);
    void deleteByUserEmailAndTargetUrl(String email, String targetUrl);
    long deleteByUserEmailAndReadAtIsNotNull(String email);
}
