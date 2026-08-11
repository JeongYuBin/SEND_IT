package com.sendit.place;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSavedPlaceSourceRepository
        extends JpaRepository<UserSavedPlaceSource, Long> {
    boolean existsBySavedPlaceIdAndSharedContentId(Long savedPlaceId, Long sharedContentId);
    List<UserSavedPlaceSource> findBySavedPlaceIdOrderByLinkedAtDesc(Long savedPlaceId);
}
