package com.example.jobqueue.kafka;

import com.example.jobqueue.model.Job;
import com.example.jobqueue.model.JobStatus;
import com.example.jobqueue.service.JobProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for distributed job processing
 * Implements Single Responsibility: only consumes messages and delegates to processing service
 * Implements Dependency Inversion: depends on JobProcessingService abstraction
 */
@Service
public class JobConsumer {

    private static final String JOBS_TOPIC = "job-submissions";
    private final JobProcessingService jobProcessingService;

    public JobConsumer(JobProcessingService jobProcessingService) {
        this.jobProcessingService = jobProcessingService;
    }

    @KafkaListener(topics = JOBS_TOPIC, groupId = "job-processor")
    public void processJobFromKafka(Job job) {
        if (job.getStatus() == JobStatus.QUEUED) {
            jobProcessingService.process(job);
        }
    }
}
