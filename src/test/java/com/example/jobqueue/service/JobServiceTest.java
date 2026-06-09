package com.example.jobqueue.service;

import com.example.jobqueue.model.Job;
import com.example.jobqueue.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JobServiceTest {

    @Autowired
    private JobService jobService;

    @Test
    void processJobCompletesWhenNoErrorIsSimulated() {
        Job job = new Job("test-job", Map.of("message", "hello"), 3);
        jobService.processJob(job);

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getResult()).contains("hello");
        assertThat(job.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void processJobRetriesTransientFailuresUntilLimit() {
        Job job = new Job("retry-job", Map.of("simulateError", "transient"), 3);

        while (job.getStatus() == JobStatus.QUEUED) {
            jobService.processJob(job);
        }

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getAttemptCount()).isEqualTo(3);
        assertThat(job.getErrorMessage()).contains("Transient failure after retries");
    }

    @Test
    void processJobFailsImmediatelyForPermanentErrors() {
        Job job = new Job("permanent-job", Map.of("simulateError", "permanent"), 3);
        jobService.processJob(job);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getErrorMessage()).contains("Permanent failure");
    }
}
