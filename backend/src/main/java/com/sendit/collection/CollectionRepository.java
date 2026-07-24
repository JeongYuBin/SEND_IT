package com.sendit.collection;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByUserEmailOrderByCreatedAtDesc(String email);
    Optional<Collection> findByIdAndUserEmail(Long id, String email);
}

