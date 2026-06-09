# Job Processing Scheduler

A Spring Boot REST API with an asynchronous background worker queue. Supports both in-memory queuing and **Apache Kafka** for distributed processing.

## Architecture

The system consists of:

- `JobController`: accepts HTTP POST submissions and returns a `jobId` immediately.
- `JobService`: stores job state in a thread-safe `ConcurrentHashMap` and queues jobs using either `LinkedBlockingQueue` (local) or **Kafka** (distributed).
- `JobWorker` / `JobConsumer`: worker pool that processes jobs in the background.

### Queue Modes

#### Local Mode (Default)
```text
Client HTTP POST /api/jobs
          |
          v
    JobController
          |
          v
    JobService.submitJob()
    - persist job state
    - enqueue locally
          |
          v
   background worker pool
  (LinkedBlockingQueue)
          |
          v
     process payload
  (sleep / simulate errors)
          |
          v
     update job status
```

#### Kafka Mode (Distributed)
```text
Client HTTP POST /api/jobs
          |
          v
    JobController
          |
          v
    JobService.submitJob()
    - persist job state
    - send to Kafka
          |
          v
     Kafka Broker
    (job-submissions topic)
          |
    +-----+-----+
    |     |     |
    v     v     v
 Consumer 1,2,3
  (parallel processing)
    |     |     |
    +-----+-----+
          |
          v
 JobService.processJobAsync()
    update job status
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
- Docker (optional, for Kafka)

### Quick Start (Local Mode)

```bash
cd /workspaces/JobProcessingScheduler
mvn clean package
java -jar target/job-processing-scheduler-0.0.1-SNAPSHOT.jar
```

Server runs on `http://localhost:8080`

### With Kafka (Distributed Mode)

```bash
cd /workspaces/JobProcessingScheduler

# Start Kafka + API
docker-compose up -d

# Wait for Kafka to initialize (~10-15 seconds)
sleep 20

# Verify
curl http://localhost:8080/api/jobs
```

See [KAFKA.md](KAFKA.md) for detailed Kafka configuration and multi-instance deployment.

### Run tests

```bash
mvn test
```

## Configuration

### Enable Kafka

```yaml
# application.yml
job:
  queue:
    use-kafka: true
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

Or via environment variable:
```bash
java -Djob.queue.use-kafka=true \
     -Dspring.kafka.bootstrap-servers=kafka:9092 \
     -jar target/job-processing-scheduler-0.0.1-SNAPSHOT.jar
```

## Known limitations

### Local Mode
- Uses in-memory storage; state is lost on restart
- Single-instance only (no horizontal scaling)

### Kafka Mode
- Requires external Kafka broker
- Job state not persisted to database (use ConcurrentHashMap only)
- No built-in monitoring dashboard

## Extensions

- Add persistent job storage (PostgreSQL, MongoDB)
- Add job cancellation and priority-based processing
- Add deployment manifests for DigitalOcean, Kubernetes, or Docker
- Add distributed tracing (OpenTelemetry)
- Add metrics collection (Micrometer/Prometheus)

