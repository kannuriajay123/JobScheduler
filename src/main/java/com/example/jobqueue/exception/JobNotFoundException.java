package com.example.jobqueue.exception;

/**
 * Job not found in repository
 * Implements Single Responsibility: represents lookup failure
 */
public class JobNotFoundException extends JobException {
    public JobNotFoundException(String jobId) {
        super("Job not found: " + jobId);
    }
}
