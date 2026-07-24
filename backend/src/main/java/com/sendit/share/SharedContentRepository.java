package com.sendit.share;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedContentRepository extends JpaRepository<SharedContent, Long> {

    Optional<SharedContent> findByUserIdAndNormalizedUrl(Long userId, String normalizedUrl);

    Optional<SharedContent> findByIdAndUserEmail(Long id, String email);
}

