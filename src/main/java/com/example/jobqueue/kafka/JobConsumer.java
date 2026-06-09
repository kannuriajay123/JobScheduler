package com.example.jobqueue.kafka;

import com.example.jobqueue.model.Job;
import com.example.jobqueue.model.JobStatus;
import com.example.jobqueue.service.JobService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class JobConsumer {

    private static final String JOBS_TOPIC = "job-submissions";
    private final JobService jobService;

    public JobConsumer(JobService jobService) {
        this.jobService = jobService;
    }

    @KafkaListener(topics = JOBS_TOPIC, groupId = "job-processor")
    public void processJobFromKafka(Job job) {
        if (job.getStatus() == JobStatus.QUEUED) {
            jobService.processJobAsync(job);
        }
    }
}
