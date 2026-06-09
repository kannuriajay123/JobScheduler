package com.example.jobqueue.repository;

import com.example.jobqueue.model.Job;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of JobRepository
 * Implements Open/Closed Principle: open for extension (can add DB implementation), closed for modification
 * Implements Single Responsibility: only handles job persistence
 */
@Repository
public class InMemoryJobRepository implements JobRepository {
    
    private final ConcurrentMap<String, Job> jobStore = new ConcurrentHashMap<>();
    
    @Override
    public void save(Job job) {
        jobStore.put(job.getId(), job);
    }
    
    @Override
    public Optional<Job> findById(String jobId) {
        return Optional.ofNullable(jobStore.get(jobId));
    }
    
    @Override
    public boolean exists(String jobId) {
        return jobStore.containsKey(jobId);
    }
}
