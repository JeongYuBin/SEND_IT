package com.sendit.itinerary;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    List<Itinerary> findByUserEmailOrderByStartDateDescCreatedAtDesc(String email);
    Optional<Itinerary> findByIdAndUserEmail(Long id, String email);
}
