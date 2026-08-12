package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendit.notification.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class AnalysisJobServiceTest {
    @Test
    void returnsStaleProcessingJobToPendingQueue() {
        AnalysisJobRepository jobs = mock(AnalysisJobRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        AnalysisJob job = new AnalysisJob(mock(SharedContent.class));
        job.start(java.time.Instant.now().minusSeconds(3600));
        when(jobs.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                eq(JobStatus.PROCESSING), any(), any(Pageable.class)))
                .thenReturn(List.of(job));
        AnalysisJobService service = new AnalysisJobService(jobs, notifications, 2, 1800);

        service.recoverStaleJobs();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getRetryCount()).isEqualTo(1);
        verify(jobs).findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                eq(JobStatus.PROCESSING), any(), any(Pageable.class));
    }
}
