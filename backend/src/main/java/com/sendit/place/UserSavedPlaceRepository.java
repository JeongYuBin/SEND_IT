package com.sendit.place;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSavedPlaceRepository extends JpaRepository<UserSavedPlace, Long> {
    List<UserSavedPlace> findByUserEmailOrderBySavedAtDesc(String email);
    Optional<UserSavedPlace> findByIdAndUserEmail(Long id, String email);
    boolean existsByUserIdAndPlaceId(Long userId, Long placeId);
}

