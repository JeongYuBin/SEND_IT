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
        AnalysisJobService service = new AnalysisJobService(jobs, notifications, mock(com.sendit.place.SavedPlaceService.class), mock(SharedContentRepository.class), 2, 1800);

        service.recoverStaleJobs();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getRetryCount()).isEqualTo(1);
        verify(jobs).findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                eq(JobStatus.PROCESSING), any(), any(Pageable.class));
    }
    @Test
    void savesPlacesBeforeNotifyingCompletionAndPropagatesSaveFailures() {
        var jobs = mock(AnalysisJobRepository.class);
        var notifications = mock(NotificationService.class);
        var saver = mock(com.sendit.place.SavedPlaceService.class);
        var contents = mock(SharedContentRepository.class);
        var content = mock(SharedContent.class);
        when(content.getId()).thenReturn(9L);
        var job = new AnalysisJob(content);
        when(jobs.findById(1L)).thenReturn(java.util.Optional.of(job));
        when(contents.findForSaving(9L)).thenReturn(java.util.Optional.of(content));
        var service = new AnalysisJobService(jobs, notifications, saver, contents, 2, 1800);
        var metadata = new PageMetadata("카페", null, null, "돌담카페", "카페", "제주", 33.4, 126.5);

        service.complete(1L, metadata, false);

        var order = org.mockito.Mockito.inOrder(contents, content, saver, notifications);
        order.verify(contents).findForSaving(9L);
        order.verify(content).completeAnalysis(metadata);
        order.verify(saver).autoSaveAnalyzedShare(9L);
        order.verify(notifications).notifyAnalysisResult(content);

        org.mockito.Mockito.clearInvocations(notifications);
        org.mockito.Mockito.doThrow(new IllegalStateException("save failed")).when(saver).autoSaveAnalyzedShare(9L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.complete(1L, metadata, false))
                .isInstanceOf(IllegalStateException.class);
        org.mockito.Mockito.verifyNoInteractions(notifications);
    }
}
