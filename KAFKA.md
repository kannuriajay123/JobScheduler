# Kafka Integration Guide

## Overview

The Job Processing Scheduler now supports **Apache Kafka** for distributed, asynchronous job processing. This provides:

- **Persistence**: Jobs survive broker restarts
- **Scalability**: Multiple instances consume jobs independently
- **Ordering**: Partition keys ensure job ordering per job ID
- **Reliability**: Automatic retries and error handling
- **Monitoring**: Kafka-native tools for topic inspection

---

## Architecture

```
[Client]
   |
   v
[Spring Boot API]
   |
   +-> Kafka Producer (topic: job-submissions)
   |
   v
[Kafka Broker]
   |
   +-> [Consumer 1] -> [JobService.processJobAsync()]
   +-> [Consumer 2] -> [JobService.processJobAsync()]
   +-> [Consumer 3] -> [JobService.processJobAsync()]
   |
   v
[Job Store (ConcurrentHashMap)]
```

---

## Quick Start

### 1. Start Kafka with Docker Compose

```bash
cd /workspaces/JobProcessingScheduler
docker-compose up -d
```

This starts:
- **Zookeeper** on port 2181
- **Kafka** on port 9092
- **Job API** on port 8080 (auto-configured to use Kafka)

### 2. Verify Kafka is Running

```bash
# Check if Kafka is ready (wait 10-15 seconds)
docker-compose logs kafka | grep "started"
```

### 3. Submit a Job

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"message":"hello world"}}'
```

Response:
```json
{"jobId":"550e8400-e29b-41d4-a716-446655440000"}
```

### 4. Check Job Status

```bash
curl http://localhost:8080/api/jobs/550e8400-e29b-41d4-a716-446655440000
```

Response:
```json
{
  "jobId":"550e8400-e29b-41d4-a716-446655440000",
  "status":"COMPLETED",
  "result":"Result payload={message=hello world}",
  "errorMessage":null,
  "attempts":1
}
```

---

## Configuration

### Enable/Disable Kafka

In `application.yml`:
```yaml
job:
  queue:
    use-kafka: true  # Set to false to use in-memory queue
```

Or as environment variable:
```bash
export job.queue.use-kafka=true
```

### Kafka Properties

In `application.yml`:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all              # Wait for all replicas
      retries: 3             # Retry failed sends
    consumer:
      group-id: job-processor
      enable-auto-commit: true
      max-poll-records: 100
```

---

## Kafka Topics

### `job-submissions` (Primary Topic)

- **Partitions**: 3
- **Replication Factor**: 1
- **Retention**: 7 days (default)
- **Message Format**: JSON (Job object)

Partition key: `jobId` (ensures ordering per job)

---

## Monitoring Kafka

### List Topics

```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Describe a Topic

```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic job-submissions
```

### Consume Messages (for debugging)

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic job-submissions \
  --from-beginning
```

### Check Consumer Group Status

```bash
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group job-processor \
  --describe
```

---

## Error Handling & Retries

The system handles failures at two levels:

### 1. **Transient Failures** (Within Job Processing)
- Automatically retries up to 3 times
- 250ms delay between retries
- Example: Network timeout, temporary service unavailable
- Trigger: `"simulateError":"transient"` in payload

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"simulateError":"transient"}}'
```

### 2. **Permanent Failures** (Immediate Failure)
- No retries
- Status marked as FAILED
- Example: Invalid job format, unsupported operation
- Trigger: `"simulateError":"permanent"` in payload

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"simulateError":"permanent"}}'
```

### 3. **Kafka Producer Failures**
- Kafka retries 3 times before failing
- Exponential backoff with jitter
- If all retries fail, an exception is raised

---

## Multi-Instance Deployment

With Kafka, you can run multiple instances:

```bash
# Terminal 1: Start Kafka
docker-compose up kafka zookeeper

# Terminal 2: Start instance 1
java -Dspring.kafka.bootstrap-servers=localhost:9092 \
     -Djob.queue.use-kafka=true \
     -jar target/job-processing-scheduler-0.0.1-SNAPSHOT.jar

# Terminal 3: Start instance 2
java -Dspring.kafka.bootstrap-servers=localhost:9092 \
     -Djob.queue.use-kafka=true \
     -jar target/job-processing-scheduler-0.0.1-SNAPSHOT.jar

# Terminal 4: Start instance 3
java -Dspring.kafka.bootstrap-servers=localhost:9092 \
     -Djob.queue.use-kafka=true \
     -jar target/job-processing-scheduler-0.0.1-SNAPSHOT.jar
```

All instances share the same consumer group (`job-processor`) and automatically distribute jobs across instances.

---

## Switching Between Queue Modes

### Local Mode (In-Memory Queue)
```yaml
job:
  queue:
    use-kafka: false
```
- ✅ No external dependencies
- ❌ Single instance only
- ❌ Lost on restart

### Kafka Mode (Distributed)
```yaml
job:
  queue:
    use-kafka: true
```
- ✅ Multi-instance support
- ✅ Persistent across restarts
- ✅ Better scalability
- ⚠️ Requires Kafka broker

---

## Troubleshooting

### Kafka Connection Refused

```
Error: java.net.ConnectException: Connection refused
```

**Solution**: Ensure Kafka is running:
```bash
docker-compose ps
docker-compose logs kafka
```

### Messages Not Being Processed

**Check consumer group lag**:
```bash
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group job-processor \
  --describe
```

If lag is increasing, check logs:
```bash
docker-compose logs job-api | grep -i error
```

### Topic Not Found

```
Error: Topic 'job-submissions' not found
```

**Solution**: The topic auto-creates when the first message is sent. To manually create:
```bash
docker-compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic job-submissions \
  --partitions 3 \
  --replication-factor 1
```

---

## Performance Tuning

### Increase Partitions (for higher throughput)
```bash
docker-compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --alter \
  --topic job-submissions \
  --partitions 6
```

### Adjust Consumer Concurrency
In `KafkaConfig.java`:
```java
factory.setConcurrency(6);  // Increase from 3
```

### Batch Processing
In `application.yml`:
```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 500  # Process up to 500 per poll
```

---

## Production Considerations

For production deployments:

1. **Use Kafka Cluster** (multiple brokers for HA)
2. **Enable Replication** (replication-factor > 1)
3. **Monitor Consumer Lag** (via Prometheus/JMX)
4. **Set Retention Policies** (based on business needs)
5. **Use Authentication** (SASL/SSL)
6. **Enable Metrics** (via Micrometer)
7. **Backup Job Store** (persist to database)

---

## Cleanup

### Stop Services
```bash
docker-compose down
```

### Remove Volumes (clears Kafka data)
```bash
docker-compose down -v
```

---

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Confluent Docker Images](https://hub.docker.com/r/confluentinc/cp-kafka)
