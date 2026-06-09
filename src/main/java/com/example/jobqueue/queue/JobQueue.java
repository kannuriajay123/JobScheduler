package com.example.jobqueue.queue;

import com.example.jobqueue.model.Job;

/**
 * Strategy pattern: abstracts job queue implementation
 * Implements Interface Segregation: minimal queue interface
 * Implements Open/Closed: new implementations (Kafka, RabbitMQ) can be added without modifying JobService
 * Implements Dependency Inversion: services depend on this abstraction
 */
public interface JobQueue {
    
    /**
     * Submit a job to the queue
     */
    void enqueue(Job job);
    
    /**
     * Initialize the queue (e.g., start consumers)
     */
    void start();
    
    /**
     * Shutdown the queue gracefully
     */
    void shutdown();
}
