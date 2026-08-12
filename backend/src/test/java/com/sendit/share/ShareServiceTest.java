package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendit.notification.NotificationService;
import com.sendit.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShareServiceTest {
    @Test
    void doesNotCreateDuplicateJobWhileAnalysisIsActive() {
        UserRepository users = mock(UserRepository.class);
        SharedContentRepository contents = mock(SharedContentRepository.class);
        AnalysisJobRepository jobs = mock(AnalysisJobRepository.class);
        SharedContent content = mock(SharedContent.class);
        when(contents.findByIdAndUserEmail(7L, "user@example.com"))
                .thenReturn(Optional.of(content));
        when(content.getId()).thenReturn(7L);
        when(content.getAnalysisStatus()).thenReturn(AnalysisStatus.ANALYZING);
        when(jobs.existsBySharedContentIdAndStatusIn(
                7L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).thenReturn(true);
        ShareService service = new ShareService(users, contents, jobs,
                mock(UrlNormalizer.class), mock(MediaStorageCleaner.class),
                mock(NotificationService.class), mock(SharedContentPlaceRepository.class));

        ShareDtos.ShareAcceptedResponse response = service.reanalyze(
                "user@example.com", 7L);

        assertThat(response.duplicate()).isTrue();
        assertThat(response.status()).isEqualTo(AnalysisStatus.ANALYZING);
        verify(content, never()).queueForAnalysis();
        verify(jobs, never()).save(any(AnalysisJob.class));
    }
}
