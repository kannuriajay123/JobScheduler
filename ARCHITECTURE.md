# SOLID Architecture & Design Patterns

This document explains how the Job Processing Scheduler implements SOLID principles and design patterns for clean, maintainable code.

---

## SOLID Principles Applied

### 1. **Single Responsibility Principle (SRP)**

Each class has one reason to change:

- **`JobService`** (interface) → `DefaultJobService` (impl)
  - **Responsibility**: Job submission and status retrieval
  - **Why**: Decoupled from job processing, queue management, and storage

- **`JobProcessingService`** (interface) → `DefaultJobProcessingService` (impl)
  - **Responsibility**: Job execution and retry logic
  - **Why**: Focused only on what happens during job processing

- **`JobRepository`** (interface) → `InMemoryJobRepository` (impl)
  - **Responsibility**: Job persistence
  - **Why**: Isolated from business logic; can be swapped for database implementation

- **`JobQueue`** (interface) → `InMemoryJobQueue` / `KafkaJobQueue` (impl)
  - **Responsibility**: Job queuing mechanism
  - **Why**: Abstracted from submission/processing logic

- **`JobConsumer`** (Kafka consumer)
  - **Responsibility**: Consuming messages from Kafka topic
  - **Why**: Single reason to change: message format or Kafka protocol changes

- **Exception Hierarchy**
  - `TransientJobException` → Failures that can be retried
  - `PermanentJobException` → Failures that should not be retried
  - `JobNotFoundException` → Job lookup failure
  - **Why**: Each exception represents a distinct failure type

---

### 2. **Open/Closed Principle (OCP)**

Classes are **open for extension, closed for modification**:

- **Queue Implementations**
  - `InMemoryJobQueue` - Local processing with fixed thread pool
  - `KafkaJobQueue` - Distributed processing via Kafka
  - **How**: Both implement `JobQueue` interface
  - **Benefit**: Add `RabbitMQJobQueue` without modifying existing code

- **Repository Implementations**
  - `InMemoryJobRepository` - Current implementation
  - **Future**: `PostgresJobRepository`, `MongoJobRepository`
  - **How**: Both implement `JobRepository` interface
  - **Benefit**: Switch storage backends by configuration only

- **Processing Service**
  - `DefaultJobProcessingService` - Current implementation
  - **Future**: `EnhancedJobProcessingService`, `CachedJobProcessingService`
  - **How**: Implement `JobProcessingService` interface
  - **Benefit**: Extend behavior without modifying existing code

---

### 3. **Liskov Substitution Principle (LSP)**

Subtypes are substitutable for base types:

```java
// Both implementations can be swapped without breaking code
JobQueue queue1 = new InMemoryJobQueue(jobProcessingService);
JobQueue queue2 = new KafkaJobQueue(jobProducer);

jobService.submitJob(payload);  // Works with either queue implementation
```

**Guarantees**:
- `enqueue(Job job)` behaves identically conceptually
- `start()` and `shutdown()` follow the same contract
- No type casting required in service classes

---

### 4. **Interface Segregation Principle (ISP)**

Clients depend on focused, specific interfaces:

- **`JobService`** interface
  ```java
  public interface JobService {
      String submitJob(Map<String, Object> payload);
      Optional<JobStatusResponse> getJobStatus(String jobId);
  }
  ```
  - Only 2 methods: job submission and status retrieval
  - Clients don't depend on internal details (processing, retry logic)

- **`JobProcessingService`** interface
  ```java
  public interface JobProcessingService {
      void process(Job job);
  }
  ```
  - Single method: process a job
  - No need for job submission or repository methods

- **`JobQueue`** interface
  ```java
  public interface JobQueue {
      void enqueue(Job job);
      void start();
      void shutdown();
  }
  ```
  - Only queue operations; no processing or storage methods
  - Minimal contract for implementations

---

### 5. **Dependency Inversion Principle (DIP)**

High-level modules depend on abstractions, not concrete implementations:

```
JobController
    ↓ depends on
JobService (interface)
    ↓ depends on
JobRepository (interface)
JobQueue (interface)
    ↓ depend on
Concrete: InMemoryJobRepository, KafkaJobQueue
```

**Benefits**:
- `JobController` doesn't know about `DefaultJobService`
- `DefaultJobService` doesn't know about `InMemoryJobRepository`
- Implementations can be swapped via Spring configuration

**Configuration-driven selection**:
```yaml
job.queue.use-kafka: true  # Selects KafkaJobQueue
job.queue.use-kafka: false # Selects InMemoryJobQueue
```

---

## Design Patterns Used

### 1. **Strategy Pattern** (JobQueue)

**Problem**: Different queuing strategies needed (local vs. distributed)

**Solution**: Define `JobQueue` interface with multiple implementations

```java
public interface JobQueue {
    void enqueue(Job job);
    void start();
    void shutdown();
}

// Strategies:
class InMemoryJobQueue implements JobQueue { ... }  // Local strategy
class KafkaJobQueue implements JobQueue { ... }     // Distributed strategy
```

**Benefit**: Switch between strategies at runtime via configuration

```yaml
job.queue.use-kafka: false  # Uses InMemoryJobQueue
job.queue.use-kafka: true   # Uses KafkaJobQueue
```

---

### 2. **Repository Pattern** (JobRepository)

**Problem**: Decouple business logic from data access

**Solution**: Define `JobRepository` interface for data operations

```java
public interface JobRepository {
    void save(Job job);
    Optional<Job> findById(String jobId);
    boolean exists(String jobId);
}

// Implementations:
class InMemoryJobRepository implements JobRepository { ... }  // Cache strategy
class PostgresJobRepository implements JobRepository { ... }   // DB strategy (future)
```

**Benefit**: Can move from in-memory to persistent storage without changing service logic

---

### 3. **Dependency Injection Pattern** (Spring)

**Problem**: Tight coupling between classes

**Solution**: Inject dependencies via constructor

```java
@Service
public class DefaultJobService implements JobService {
    private final JobRepository jobRepository;
    private final JobQueue jobQueue;

    public DefaultJobService(JobRepository jobRepository, JobQueue jobQueue) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
    }
}
```

**Benefit**: Easy to test, mock, and swap implementations

---

### 4. **Factory Pattern** (Spring Auto-wiring)

**Problem**: Creating right implementation based on configuration

**Solution**: Spring's `@ConditionalOnProperty` acts as factory

```java
@Component
@ConditionalOnProperty(name = "job.queue.use-kafka", havingValue = "false", matchIfMissing = true)
public class InMemoryJobQueue implements JobQueue { ... }

@Component
@ConditionalOnProperty(name = "job.queue.use-kafka", havingValue = "true")
public class KafkaJobQueue implements JobQueue { ... }
```

**Benefit**: Automatic implementation selection without factory boilerplate

---

### 5. **Decorator/Wrapper Pattern** (Retry Logic)

**Problem**: Need to add retry behavior to job processing

**Solution**: `JobProcessingService` wraps execution with retry handling

```java
@Override
public void process(Job job) {
    try {
        job.beginAttempt();
        String output = executeJobPayload(job);  // Core responsibility
        job.markCompleted(output);
    } catch (TransientJobException error) {
        handleTransientFailure(job, error);      // Retry wrapper
    }
    // ...
}
```

**Benefit**: Separation of core logic (execution) and cross-cutting concern (retry)

---

### 6. **Observer Pattern** (Kafka Consumer)

**Problem**: React to job submissions asynchronously

**Solution**: `JobConsumer` listens to Kafka topic via `@KafkaListener`

```java
@Service
public class JobConsumer {
    @KafkaListener(topics = "job-submissions", groupId = "job-processor")
    public void processJobFromKafka(Job job) {
        jobProcessingService.process(job);
    }
}
```

**Benefit**: Loosely coupled job submission and processing; supports multi-instance

---

## Package Structure

```
com.example.jobqueue/
├── controller/
│   └── JobController.java               # HTTP endpoints (REST)
│
├── service/
│   ├── JobService.java                  # Interface: submission + status
│   ├── DefaultJobService.java           # Implementation
│   ├── JobProcessingService.java        # Interface: job execution
│   └── DefaultJobProcessingService.java # Implementation + retry logic
│
├── repository/
│   ├── JobRepository.java               # Interface: persistence
│   └── InMemoryJobRepository.java       # Implementation
│
├── queue/
│   ├── JobQueue.java                    # Interface: queuing
│   ├── InMemoryJobQueue.java            # Strategy: local queue
│   └── KafkaJobQueue.java               # Strategy: distributed queue
│
├── kafka/
│   ├── JobProducer.java                 # Kafka message producer
│   ├── JobConsumer.java                 # Kafka message consumer (Observer)
│   └── KafkaConfig.java                 # Kafka configuration
│
├── model/
│   ├── Job.java                         # Domain model
│   ├── JobStatus.java                   # Status enum
│   ├── JobRequest.java                  # REST request DTO
│   └── JobStatusResponse.java           # REST response DTO
│
├── exception/
│   ├── JobException.java                # Base exception
│   ├── TransientJobException.java       # Retryable failure
│   ├── PermanentJobException.java       # Non-retryable failure
│   └── JobNotFoundException.java        # Lookup failure
│
├── config/
│   └── JobServiceConfiguration.java     # Bean configuration
│
└── JobProcessingSchedulerApplication.java  # Spring Boot entry point
```

---

## Dependency Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       REST Client                               │
└────────────────────┬──────────────────────────────────────────┘
                     │
                     ↓
        ┌────────────────────────┐
        │   JobController        │  (REST endpoints)
        └────┬───────────────────┘
             │ depends on
             ↓
   ┌─────────────────────────┐
   │ JobService (interface)  │
   └────┬────────────────────┘
        │
        └─ implemented by ───→ DefaultJobService
              │
              ├─ depends on ──→ JobRepository (interface)
              │                     │
              │                     └─ implemented by ──→ InMemoryJobRepository
              │
              └─ depends on ──→ JobQueue (interface)
                                   │
                                   ├─ strategy: InMemoryJobQueue
                                   │                │
                                   │                └─ depends on ──→ JobProcessingService
                                   │
                                   └─ strategy: KafkaJobQueue
                                                     │
                                                     └─ via JobConsumer (listener)
                                                        depends on ──→ JobProcessingService


   ┌──────────────────────────────────────────┐
   │ JobProcessingService (interface)         │
   └────────────────────────────────────────┘
   │
   └─ implemented by ──→ DefaultJobProcessingService
        │
        └─ depends on ──→ JobQueue (lazy) ─ breaks circular dep
```

---

## Key Architectural Benefits

| Benefit | How Achieved |
|---------|-------------|
| **Testability** | Interfaces + dependency injection → easy mocking |
| **Extensibility** | Strategy & Repository patterns → new implementations |
| **Maintainability** | SRP + clear separation of concerns → focused classes |
| **Flexibility** | Configuration-driven selection → runtime switching |
| **Scalability** | Kafka integration + multi-instance support → horizontal scaling |
| **Reusability** | Focused interfaces → services can be composed differently |

---

## Future Extensions (OCP-ready)

The architecture supports these extensions without modifying existing code:

1. **PostgreSQL Persistence**
   ```java
   @Repository
   public class PostgresJobRepository implements JobRepository { ... }
   ```

2. **RabbitMQ Queue**
   ```java
   @Component
   public class RabbitMQJobQueue implements JobQueue { ... }
   ```

3. **Enhanced Processing (with caching)**
   ```java
   @Service
   public class CachedJobProcessingService implements JobProcessingService { ... }
   ```

4. **Job Cancellation**
   ```java
   public interface JobCancellationService {
       void cancelJob(String jobId);
   }
   ```

5. **Job Priority Queue**
   ```java
   public interface PrioritizedJobQueue extends JobQueue {
       void enqueueWithPriority(Job job, int priority);
   }
   ```

All can be added without changing `JobService`, `JobController`, or `JobProcessingService`.
