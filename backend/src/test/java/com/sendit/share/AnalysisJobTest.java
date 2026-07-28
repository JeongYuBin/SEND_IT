package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AnalysisJobTest {

    @Test
    void retriesTransientFailuresBeforeMarkingJobAsFailed() {
        AnalysisJob job = new AnalysisJob(mock(SharedContent.class));

        job.start(Instant.now());
        job.retryOrFail(Instant.now(), "temporary network failure", 2);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getRetryCount()).isEqualTo(1);

        job.start(Instant.now());
        job.retryOrFail(Instant.now(), "temporary network failure", 2);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getRetryCount()).isEqualTo(2);

        job.start(Instant.now());
        job.retryOrFail(Instant.now(), "still failing", 2);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getRetryCount()).isEqualTo(2);
    }
}
