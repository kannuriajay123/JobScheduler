package com.example.jobqueue.service;

import com.example.jobqueue.model.JobStatusResponse;
import java.util.Map;
import java.util.Optional;

/**
 * Job submission and status retrieval service
 * Implements Single Responsibility: orchestrates job lifecycle (submit, query status)
 * Implements Interface Segregation: focused interface for job management
 * Implements Dependency Inversion: depends on JobRepository and JobQueue abstractions
 */
public interface JobService {
    
    /**
     * Submit a new job for processing
     * @return job ID
     */
    String submitJob(Map<String, Object> payload);
    
    /**
     * Get the status of a job
     */
    Optional<JobStatusResponse> getJobStatus(String jobId);
}
