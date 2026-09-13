package com.sendit.place;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSavedPlaceRepository extends JpaRepository<UserSavedPlace, Long> {
    List<UserSavedPlace> findByCollectionId(Long collectionId);

    @org.springframework.data.jpa.repository.Query("select distinct s from UserSavedPlace s where s.user.id = :userId and (s.sharedContent.id = :shareId or s.id in (select source.savedPlace.id from UserSavedPlaceSource source where source.sharedContent.id = :shareId))")
    List<UserSavedPlace> findAllLinkedToShare(@org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("shareId") Long shareId);
    List<UserSavedPlace> findByUserEmailOrderBySavedAtDesc(String email);
    Optional<UserSavedPlace> findByIdAndUserEmail(Long id, String email);
    List<UserSavedPlace> findByIdInAndUserEmail(List<Long> ids, String email);
    boolean existsByUserIdAndPlaceId(Long userId, Long placeId);
    Optional<UserSavedPlace> findByUserIdAndPlaceId(Long userId, Long placeId);
    boolean existsByUserIdAndSharedContentId(Long userId, Long sharedContentId);
    List<UserSavedPlace> findByUserIdAndSharedContentId(Long userId, Long sharedContentId);
}
