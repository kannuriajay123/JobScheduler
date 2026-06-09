package com.example.jobqueue.service;

import com.example.jobqueue.exception.PermanentJobException;
import com.example.jobqueue.exception.TransientJobException;
import com.example.jobqueue.model.Job;
import com.example.jobqueue.queue.JobQueue;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default implementation of job processing
 * Implements Single Responsibility: focuses only on job execution and retry logic
 * Implements Open/Closed: behavior can be extended via subclassing or decoration
 * 
 * @Lazy breaks circular dependency: JobQueue → JobProcessingService → JobQueue
 */
@Service
public class DefaultJobProcessingService implements JobProcessingService {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(250);
    private static final long SIMULATE_WORK_MIN_MS = 300;
    private static final long SIMULATE_WORK_MAX_MS = 700;
    private static final double TRANSIENT_FAILURE_RATE = 0.12;

    @Lazy
    private final JobQueue jobQueue;

    public DefaultJobProcessingService(@Lazy JobQueue jobQueue) {
        this.jobQueue = jobQueue;
    }

    @Override
    public void process(Job job) {
        try {
            job.beginAttempt();
            String output = executeJobPayload(job);
            job.markCompleted(output);
        } catch (TransientJobException transientError) {
            handleTransientFailure(job, transientError);
        } catch (PermanentJobException permanentError) {
            job.markFailed("Permanent failure: " + permanentError.getMessage());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            job.markFailed("Worker interrupted while processing.");
        } catch (Exception unexpected) {
            job.markFailed("Unexpected failure: " + unexpected.getMessage());
        }
    }

    /**
     * Retry logic for transient failures
     * Implements Decorator pattern: wraps job with retry behavior
     */
    private void handleTransientFailure(Job job, TransientJobException error) {
        if (job.hasRemainingRetries()) {
            job.retry();
            requeueWithDelay(job);
        } else {
            job.markFailed("Transient failure after retries: " + error.getMessage());
        }
    }

    /**
     * Simulate job execution with random failures
     * Implements Single Responsibility: only job payload execution
     */
    private String executeJobPayload(Job job) 
            throws InterruptedException, TransientJobException, PermanentJobException {
        // Simulate background work delay
        Thread.sleep(ThreadLocalRandom.current().nextLong(SIMULATE_WORK_MIN_MS, SIMULATE_WORK_MAX_MS));

        // Check for simulated errors in payload
        Object simulateError = job.getPayload().get("simulateError");
        if ("transient".equals(simulateError)) {
            throw new TransientJobException("Simulated transient error");
        }
        if ("permanent".equals(simulateError)) {
            throw new PermanentJobException("Simulated permanent error");
        }

        // Random transient failure for testing
        if (ThreadLocalRandom.current().nextDouble() < TRANSIENT_FAILURE_RATE) {
            throw new TransientJobException("Random transient error");
        }

        return "Result payload=" + job.getPayload().toString();
    }

    /**
     * Requeue job after delay
     * Abstracts retry mechanism - works with any JobQueue implementation
     */
    private void requeueWithDelay(Job job) {
        try {
            Thread.sleep(RETRY_DELAY.toMillis());
            jobQueue.enqueue(job);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            job.markFailed("Retry interrupted while requeueing.");
        }
    }
}
