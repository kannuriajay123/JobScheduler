package com.example.jobqueue.service;

import com.example.jobqueue.model.Job;
import com.example.jobqueue.model.JobStatus;
import com.example.jobqueue.model.JobStatusResponse;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class JobService {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int WORKER_COUNT = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(250);

    private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<String, Job> jobStore = new ConcurrentHashMap<>();
    private final ExecutorService workerPool = Executors.newFixedThreadPool(WORKER_COUNT);

    @PostConstruct
    public void startWorkers() {
        for (int i = 0; i < WORKER_COUNT; i++) {
            workerPool.execute(new JobWorker());
        }
    }

    @PreDestroy
    public void shutdownWorkers() {
        workerPool.shutdownNow();
    }

    public String submitJob(Map<String, Object> payload) {
        String id = UUID.randomUUID().toString();
        Job job = new Job(id, payload, DEFAULT_MAX_RETRIES);
        jobStore.put(id, job);
        jobQueue.offer(job);
        return id;
    }

    public Optional<JobStatusResponse> getJobStatus(String jobId) {
        Job job = jobStore.get(jobId);
        if (job == null) {
            return Optional.empty();
        }
        return Optional.of(new JobStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getResult(),
                job.getErrorMessage(),
                job.getAttemptCount()
        ));
    }

    void processJob(Job job) {
        try {
            job.beginAttempt();
            String output = executeJobPayload(job);
            job.markCompleted(output);
        } catch (TransientJobException transientError) {
            if (job.hasRemainingRetries()) {
                job.retry();
                requeueWithDelay(job);
            } else {
                job.markFailed("Transient failure after retries: " + transientError.getMessage());
            }
        } catch (PermanentJobException permanentError) {
            job.markFailed("Permanent failure: " + permanentError.getMessage());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            job.markFailed("Worker interrupted while processing.");
        } catch (Exception unexpected) {
            job.markFailed("Unexpected failure: " + unexpected.getMessage());
        }
    }

    private String executeJobPayload(Job job) throws InterruptedException, TransientJobException, PermanentJobException {
        // Simulate background work delay.
        Thread.sleep(ThreadLocalRandom.current().nextLong(300, 700));
        Object simulateError = job.getPayload().get("simulateError");
        if ("transient".equals(simulateError)) {
            throw new TransientJobException("Simulated transient error");
        }
        if ("permanent".equals(simulateError)) {
            throw new PermanentJobException("Simulated permanent error");
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.12) {
            throw new TransientJobException("Random transient error");
        }
        return "Result payload=" + job.getPayload().toString();
    }

    private void requeueWithDelay(Job job) {
        try {
            Thread.sleep(RETRY_DELAY.toMillis());
            jobQueue.offer(job);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            job.markFailed("Retry interrupted while requeueing.");
        }
    }

    private class JobWorker implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Job job = jobQueue.take();
                    processJob(job);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static final class TransientJobException extends Exception {
        TransientJobException(String message) {
            super(message);
        }
    }

    static final class PermanentJobException extends Exception {
        PermanentJobException(String message) {
            super(message);
        }
    }
}
