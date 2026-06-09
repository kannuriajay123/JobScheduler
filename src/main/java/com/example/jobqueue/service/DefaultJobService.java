package com.example.jobqueue.service;

import com.example.jobqueue.model.Job;
import com.example.jobqueue.model.JobStatusResponse;
import com.example.jobqueue.queue.JobQueue;
import com.example.jobqueue.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of JobService
 * Implements Single Responsibility: only handles job submission and status retrieval
 * Implements Dependency Inversion: depends on JobRepository and JobQueue abstractions
 * Implements Open/Closed: behavior can be extended or alternative implementations provided
 */
@Service
public class DefaultJobService implements JobService {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private final JobRepository jobRepository;
    private final JobQueue jobQueue;

    public DefaultJobService(JobRepository jobRepository, JobQueue jobQueue) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
    }

    @Override
    public String submitJob(Map<String, Object> payload) {
        String jobId = UUID.randomUUID().toString();
        Job job = new Job(jobId, payload, DEFAULT_MAX_RETRIES);
        job.markQueued();
        
        // Persist job
        jobRepository.save(job);
        
        // Enqueue for processing
        jobQueue.enqueue(job);
        
        return jobId;
    }

    @Override
    public Optional<JobStatusResponse> getJobStatus(String jobId) {
        return jobRepository.findById(jobId)
                .map(job -> new JobStatusResponse(
                        job.getId(),
                        job.getStatus(),
                        job.getResult(),
                        job.getErrorMessage(),
                        job.getAttemptCount()
                ));
    }
}
