package com.sendit.share;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AnalysisJob> findByStatusOrderByCreatedAtAsc(JobStatus status, Pageable pageable);
}

