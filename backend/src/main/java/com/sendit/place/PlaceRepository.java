package com.sendit.place;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findFirstByNormalizedNameAndLatitudeAndLongitude(
            String normalizedName,
            Double latitude,
            Double longitude
    );
}
