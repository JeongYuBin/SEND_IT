package com.sendit.share;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedContentPlaceRepository extends JpaRepository<SharedContentPlace, Long> {
    List<SharedContentPlace> findBySharedContentIdOrderByDisplayOrder(Long sharedContentId);
    void deleteBySharedContentId(Long sharedContentId);
}
