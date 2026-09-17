package com.sendit.share;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SharedContentRepository extends JpaRepository<SharedContent, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from SharedContent s where s.id = :id")
    Optional<SharedContent> findForSaving(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<SharedContent> findByUserIdAndNormalizedUrl(Long userId, String normalizedUrl);

    Optional<SharedContent> findByIdAndUserEmail(Long id, String email);

    List<SharedContent> findAllByUserEmailOrderByCreatedAtDesc(String email);
    Page<SharedContent> findByUserEmail(String email, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("""
            select s from SharedContent s where s.user.email = :email
            and s.targetCollection is not null
            and (:collectionId is null or s.targetCollection.id = :collectionId)
            """)
    Page<SharedContent> findSavedPosts(
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("collectionId") Long collectionId,
            Pageable pageable);
    List<SharedContent> findByTargetCollectionId(Long collectionId);
}
