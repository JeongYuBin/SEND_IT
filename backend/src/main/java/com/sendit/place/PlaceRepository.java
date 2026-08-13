package com.sendit.place;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findFirstByNormalizedNameAndLatitudeAndLongitude(
            String normalizedName,
            Double latitude,
            Double longitude
    );
    Optional<Place> findFirstByKakaoPlaceId(String kakaoPlaceId);
    Optional<Place> findFirstByTourismContentId(String tourismContentId);
    Optional<Place> findFirstByNormalizedNameAndRoadAddress(String normalizedName, String roadAddress);
    Optional<Place> findFirstByNormalizedNameAndAddress(String normalizedName, String address);

    @Query(value = """
            SELECT * FROM places
            WHERE normalized_name = :normalizedName
              AND geography IS NOT NULL
              AND ST_DWithin(
                  geography,
                  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                  :distanceMeters
              )
            ORDER BY ST_Distance(
                geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            )
            LIMIT 1
            """, nativeQuery = true)
    Optional<Place> findNearbyDuplicate(
            @Param("normalizedName") String normalizedName,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("distanceMeters") Double distanceMeters
    );
}
