package com.example.jobqueue.kafka;

import com.example.jobqueue.model.Job;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobProducer {

    private static final String JOBS_TOPIC = "job-submissions";
    private final KafkaTemplate<String, Job> kafkaTemplate;

    public JobProducer(KafkaTemplate<String, Job> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendJob(Job job) {
        // Send job to Kafka topic with jobId as key for partitioning
        kafkaTemplate.send(JOBS_TOPIC, job.getId(), job);
    }
}
