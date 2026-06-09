package com.example.jobqueue.model;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Job {

    private final String id;
    private final Map<String, Object> payload;
    private volatile JobStatus status;
    private volatile String result;
    private volatile String errorMessage;
    private final AtomicInteger attemptCount = new AtomicInteger(0);
    private final int maxRetries;

    public Job(String id, Map<String, Object> payload, int maxRetries) {
        this.id = id;
        this.payload = payload == null ? Collections.emptyMap() : Collections.unmodifiableMap(payload);
        this.status = JobStatus.QUEUED;
        this.maxRetries = maxRetries;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public synchronized JobStatus getStatus() {
        return status;
    }

    public synchronized void markQueued() {
        this.status = JobStatus.QUEUED;
    }

    public synchronized void markRunning() {
        this.status = JobStatus.RUNNING;
    }

    public synchronized void markCompleted(String result) {
        this.result = result;
        this.status = JobStatus.COMPLETED;
    }

    public synchronized void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = JobStatus.FAILED;
    }

    public synchronized void beginAttempt() {
        this.attemptCount.incrementAndGet();
        this.status = JobStatus.RUNNING;
    }

    public synchronized boolean hasRemainingRetries() {
        return attemptCount.get() < maxRetries;
    }

    public synchronized void retry() {
        this.status = JobStatus.QUEUED;
    }

    public String getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getAttemptCount() {
        return attemptCount.get();
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
