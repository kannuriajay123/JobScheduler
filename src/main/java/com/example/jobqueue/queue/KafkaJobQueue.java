package com.example.jobqueue.queue;

import com.example.jobqueue.model.Job;
import com.example.jobqueue.kafka.JobProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka-based queue for distributed job processing
 * Implements Strategy pattern: alternative to InMemoryJobQueue
 * Implements Single Responsibility: only handles Kafka job submission
 */
@Component
@ConditionalOnProperty(name = "job.queue.use-kafka", havingValue = "true")
public class KafkaJobQueue implements JobQueue {

    private final JobProducer jobProducer;

    public KafkaJobQueue(JobProducer jobProducer) {
        this.jobProducer = jobProducer;
    }

    @Override
    public void enqueue(Job job) {
        jobProducer.sendJob(job);
    }

    @Override
    public void start() {
        // Kafka consumer is managed by @KafkaListener in JobConsumer
    }

    @Override
    public void shutdown() {
        // Kafka resources managed by Spring
    }
}
