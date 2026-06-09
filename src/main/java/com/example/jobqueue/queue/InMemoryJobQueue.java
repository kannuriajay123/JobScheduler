package com.example.jobqueue.queue;

import com.example.jobqueue.model.Job;
import com.example.jobqueue.service.JobProcessingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-memory queue with local worker pool
 * Implements Strategy pattern: alternative to KafkaJobQueue
 * Implements Single Responsibility: only handles local job queuing
 */
@Component
@ConditionalOnProperty(name = "job.queue.use-kafka", havingValue = "false", matchIfMissing = true)
public class InMemoryJobQueue implements JobQueue {

    private static final int WORKER_COUNT = 3;
    private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>();
    private final ExecutorService workerPool = Executors.newFixedThreadPool(WORKER_COUNT);
    private final JobProcessingService jobProcessingService;

    public InMemoryJobQueue(JobProcessingService jobProcessingService) {
        this.jobProcessingService = jobProcessingService;
    }

    @Override
    public void enqueue(Job job) {
        jobQueue.offer(job);
    }

    @PostConstruct
    @Override
    public void start() {
        for (int i = 0; i < WORKER_COUNT; i++) {
            workerPool.execute(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Job job = jobQueue.take();
                        jobProcessingService.process(job);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
    }

    @PreDestroy
    @Override
    public void shutdown() {
        workerPool.shutdownNow();
    }
}
