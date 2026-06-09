package com.example.jobqueue.service;

import com.example.jobqueue.model.Job;

/**
 * Business logic for processing individual jobs
 * Implements Single Responsibility: only handles job execution and state transitions
 * Implements Interface Segregation: focused on job processing only
 */
public interface JobProcessingService {
    
    /**
     * Process a job with retry logic for transient failures
     */
    void process(Job job);
}
