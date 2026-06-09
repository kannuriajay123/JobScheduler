package com.example.jobqueue.repository;

import com.example.jobqueue.model.Job;
import java.util.Optional;

/**
 * Repository pattern: abstracts data access layer
 * Implements Interface Segregation: minimal, focused interface
 * Implements Dependency Inversion: services depend on this abstraction, not concrete storage
 */
public interface JobRepository {
    
    /**
     * Save or update a job
     */
    void save(Job job);
    
    /**
     * Find a job by ID
     */
    Optional<Job> findById(String jobId);
    
    /**
     * Check if a job exists
     */
    boolean exists(String jobId);
}
