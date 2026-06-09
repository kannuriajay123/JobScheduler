# Job Processing Scheduler

A Spring Boot REST API with an asynchronous background worker queue.

## Architecture

The system consists of:

- `JobController`: accepts HTTP POST submissions and returns a `jobId` immediately.
- `JobService`: stores job state in a thread-safe `ConcurrentHashMap` and queues jobs in a `LinkedBlockingQueue`.
- `JobWorker`: a fixed-size worker pool that pulls jobs from the queue and processes them in the background.

Flow diagram:

```text
Client HTTP POST /api/jobs
          |
          v
    JobController
          |
          v
    JobService.submitJob()
       - persist job state
       - enqueue job
          |
          v
   background worker pool
(LinkedBlockingQueue -> JobWorker)
          |
          v
     process payload
  (sleep / simulate errors)
          |
          v
     update job status
   (QUEUED, RUNNING, COMPLETED,
           FAILED)
```

## Endpoints

- `POST /api/jobs`
  - Request body: `{ "payload": { ... } }`
  - Response: `{ "jobId": "..." }`
  - Returns `202 Accepted` immediately.

- `GET /api/jobs/{jobId}`
  - Response includes `status`, `result`, `errorMessage`, and `attempts`.

## Setup

### Prerequisites

- Java 21
- Maven

### Build and run

```bash
cd /workspaces/JobProcessingScheduler
mvn clean package
java -jar target/job-processing-scheduler-0.0.1-SNAPSHOT.jar
```

### Run tests

```bash
cd /workspaces/JobProcessingScheduler
mvn test
```

## Known limitations

- Uses in-memory storage; state is lost if the application restarts.
- Worker pool and queue are inside the same application process, so scaling requires additional architectural changes.
- Retry logic is basic and simulates transient failures with a fixed retry count.
- No persistent audit or job history storage is included.

## Extensions

- Add durable queue backing such as RabbitMQ, Kafka, or Redis.
- Add job cancellation and priority-based processing.
- Add deployment manifests for DigitalOcean, Kubernetes, or Docker.
