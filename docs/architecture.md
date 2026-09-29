# High-Level Architecture & System Design Specification
## Distributed Task Scheduler (DTS)

---

## 1. System Overview & Problem Statement

Distributed Task Scheduler (DTS) is a horizontally scalable, fault-tolerant, event-driven background job orchestration platform built on **Java 21/25 (Spring Boot 3.3+)**, **Apache Kafka**, **Redis**, and **PostgreSQL**, with a **React + TypeScript + Vite + Tailwind CSS** developer dashboard.

### Core Objectives
1. **Dynamic Task Scheduling**: Support `ONE_TIME`, `INTERVAL`, and `CRON` schedules with IANA timezone support.
2. **Distributed Leaderless Scheduling**: Multiple scheduler replicas run concurrently; Redis distributed locks prevent duplicate task polling and enqueueing.
3. **Decoupled Execution Pipeline**: Kafka partitions task dispatching across dynamic worker consumer groups.
4. **Guaranteed Execution Semantics**: At-least-once message delivery coupled with database-level execution deduplication (`executionId` UNIQUE) delivers strict effective **exactly-once execution**.
5. **Configurable Retry & DLQ Lifecycle**: Truncated exponential backoff with jitter and automated Dead-Letter Queue (DLQ) publishing.
6. **Worker Liveness & Zombie Recovery**: Heartbeat tracking via Redis/Postgres with automated reaper daemon to recover abandoned jobs.
7. **SSRF Hardened**: Safe execution of `HTTP_TASK` and `WEBHOOK_TASK` with private IP/loopback blocking.

---

## 2. High-Level Architecture Diagram

```mermaid
flowchart TB
    subgraph Clients["Clients & Presentation Layer"]
        UI["React 18 + TS Dashboard<br/>(Vite, Tailwind, TanStack Query)"]
        API_Client["External REST API Clients / SDKs"]
    end

    subgraph Ingress["Ingress & Edge"]
        Nginx["Reverse Proxy / API Gateway<br/>(Rate Limiting & TLS Termination)"]
    end

    subgraph CoreBackend["Core Backend Cluster (Horizontally Scaled)"]
        direction TB
        subgraph APIService["API Service Instances (N Replicas)"]
            API1["API Node 1<br/>JWT Auth / Task CRUD / Manual Triggers"]
            API2["API Node 2"]
        end

        subgraph SchedulerCluster["Scheduler Service Instances (M Replicas)"]
            SCHED1["Scheduler Node 1<br/>(Lock-based Task Scanner)"]
            SCHED2["Scheduler Node 2"]
        end

        subgraph WorkerCluster["Worker Pool (K Replicas - Consumer Group)"]
            W1["Worker Node 1<br/>(Http/Webhook/Internal Executors)"]
            W2["Worker Node 2"]
            W3["Worker Node 3"]
        end
    end

    subgraph DataPlane["Data & Coordination Plane"]
        RedisCluster[("Redis Cluster / Redisson<br/>• Distributed Lock (SETNX / Watchdog)<br/>• Rate Limiting Buckets<br/>• Metadata Cache<br/>• Heartbeat TTL")]
        PostgresCluster[("PostgreSQL 16 (Primary)<br/>• Tasks & Executions (ACID)<br/>• Unique Constraints & Indexes<br/>• Optimistic Locking (@Version)<br/>• Audit Logs")]
        KafkaCluster[("Apache Kafka Event Bus<br/>• task.execution.requested<br/>• task.execution.completed<br/>• task.execution.failed<br/>• task.execution.retry<br/>• task.execution.dlq")]
    end

    subgraph ObservabilityStack["Observability & Monitoring"]
        Prometheus["Prometheus Scraper<br/>(Micrometer / Actuator)"]
        Grafana["Grafana Dashboards"]
    end

    UI --> Nginx
    API_Client --> Nginx
    Nginx --> APIService

    APIService --> PostgresCluster
    APIService --> RedisCluster

    SchedulerCluster <--> RedisCluster
    SchedulerCluster --> PostgresCluster
    SchedulerCluster --"Produce task.execution.requested"--> KafkaCluster

    KafkaCluster --"Consume Tasks"--> WorkerCluster
    WorkerCluster --> PostgresCluster
    WorkerCluster --"Heartbeat & Locks"--> RedisCluster
    WorkerCluster --"Publish Retry / DLQ / Completed"--> KafkaCluster

    CoreBackend -.-> Prometheus
    Prometheus -.-> Grafana
```

---

## 3. End-to-End Execution Sequence Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client as User / Dashboard
    participant API as API Service
    participant DB as PostgreSQL
    participant Redis as Redis (Redisson Lock)
    participant Sched as Scheduler Daemon
    participant Kafka as Kafka (Event Bus)
    participant Worker as Worker Pool

    %% Task Creation
    Client->>API: POST /api/tasks (Payload, CRON, Retry Policy)
    API->>DB: INSERT INTO tasks (status=ACTIVE, next_run_at=T0)
    API-->>Client: 201 Created (TaskId)

    %% Scheduling Loop
    loop Every 5 Seconds (Configurable Polling Window)
        Sched->>Redis: Acquire Lock: "lock:scheduler:poll" (lease=10s)
        alt Lock Acquired
            Sched->>DB: SELECT * FROM tasks WHERE status='ACTIVE' AND next_run_at <= NOW() FOR UPDATE SKIP LOCKED
            loop For Each Eligible Task
                Sched->>DB: Generate executionId (UUIDv7/v4), INSERT task_executions (QUEUED)
                Sched->>DB: UPDATE tasks SET next_run_at = calculateNext(cron), last_run_at = NOW()
                Sched->>Kafka: Publish to 'task.execution.requested' (Key=taskId, Partitioning Key)
            end
            Sched->>Redis: Release Lock
        else Lock Busy
            Sched->>Sched: Yield cycle (peer scheduler running)
        end
    end

    %% Worker Execution
    Kafka->>Worker: Consume message (taskId, executionId, payload)
    Worker->>DB: Optimistic claim execution: UPDATE task_executions SET status='RUNNING', worker_id=W1 WHERE id=executionId AND status='QUEUED'
    alt Claim Successful (Idempotent)
        Worker->>Worker: Execute Task (HTTP with SSRF validator / Internal)
        alt Execution Succeeded (HTTP 2xx)
            Worker->>DB: UPDATE task_executions SET status='SUCCESS', completed_at=NOW(), result=...
            Worker->>Kafka: Commit Offset (Manual Acknowledgment)
        else Execution Failed (HTTP 5xx / Timeout)
            Worker->>DB: UPDATE task_executions SET status='FAILED', error_message=...
            alt Attempt < maxRetries
                Worker->>DB: INSERT task_executions (status='RETRYING', attempt=attempt+1, scheduled_at=NOW() + backoff)
                Worker->>Kafka: Publish to 'task.execution.retry'
            else Attempt >= maxRetries
                Worker->>Kafka: Publish to 'task.execution.dlq'
            end
            Worker->>Kafka: Commit Offset
        end
    else Duplicate or Already Handled
        Worker->>Worker: Discard message (No-op)
        Worker->>Kafka: Commit Offset
    end
```

---

## 4. State Machines

### 4.1 Task Lifecycle State Machine
```mermaid
stateDiagram-v2
    [*] --> ACTIVE: Created (Enabled)
    [*] --> PAUSED: Created (Disabled)
    ACTIVE --> PAUSED: User pauses task
    PAUSED --> ACTIVE: User resumes task
    ACTIVE --> COMPLETED: ONE_TIME task executed successfully
    ACTIVE --> CANCELLED: User cancels task
    PAUSED --> CANCELLED: User cancels task
    ACTIVE --> FAILED: Critical terminal error
    CANCELLED --> [*]
    COMPLETED --> [*]
```

### 4.2 TaskExecution Lifecycle State Machine
```mermaid
stateDiagram-v2
    [*] --> SCHEDULED: Scheduled in database
    SCHEDULED --> QUEUED: Dispatched to Kafka
    QUEUED --> RUNNING: Worker claims execution
    RUNNING --> SUCCESS: Completed with status 2xx / valid return
    RUNNING --> FAILED: Exception thrown or timeout
    FAILED --> RETRYING: Retries remaining (attempt < maxRetries)
    RETRYING --> QUEUED: Backoff elapsed, re-enqueued
    FAILED --> DLQ: Retries exhausted
    QUEUED --> CANCELLED: Cancel requested before claim
    RUNNING --> CANCELLED: Graceful cancellation
    SUCCESS --> [*]
    DLQ --> [*]
    CANCELLED --> [*]
```

---

## 5. Distributed Systems Guarantees & Strategies

### 5.1 Idempotency Guarantee Matrix
* **Problem**: In distributed messaging systems (Kafka), network hiccups, consumer rebalances, or worker crashes cause message redelivery (**At-Least-Once** delivery).
* **Solution**: 
  1. Unique business execution identifier (`executionId` generated upstream by scheduler).
  2. Database unique constraint: `UNIQUE (execution_id)`.
  3. Worker conditional state update: `UPDATE task_executions SET status='RUNNING' WHERE execution_id = :execId AND status = 'QUEUED'`.
  4. If zero rows updated, the execution has already been claimed or processed; worker acknowledges and safely discards duplicate.

### 5.2 Concurrency Policy
* `ALLOW_CONCURRENT`: New scheduled executions start even if previous instance is still in `RUNNING` status.
* `FORBID_CONCURRENT`: Scheduler checks if any execution for this `taskId` is currently `RUNNING` or `QUEUED`. If so, skip current cycle, record audit log, and advance `next_run_at`.

### 5.3 Distributed Locking Protocol
* Implementation: **Redisson Distributed Lock** or Redis `SET resource_name my_random_value NX PX 30000`.
* Lock Lease & Watchdog: Default lease of 10s with background watchdog thread renewing TTL until transaction completes, preventing indefinite lock retention upon JVM sudden kill (`SIGKILL`).

---

## 6. Detailed Repository Directory Structure

```text
distributed-task-scheduler/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/dts/scheduler/
│       │   │   ├── Application.java
│       │   │   ├── config/             # Spring, Kafka, Redis, Security, OpenAPI configs
│       │   │   ├── security/           # JWT filter, UserDetailsService, SecurityContext
│       │   │   ├── controller/         # REST Controllers (Auth, Task, Execution, Worker, Metrics)
│       │   │   ├── service/            # Core business logic services & interfaces
│       │   │   ├── repository/         # Spring Data JPA repositories with query methods
│       │   │   ├── entity/             # JPA entity definitions with @Version, indexes
│       │   │   ├── dto/                # Request & Response Data Transfer Objects
│       │   │   ├── mapper/             # Entity-DTO mapping
│       │   │   ├── scheduler/          # Scheduler poller, CRON evaluator, lock orchestrator
│       │   │   ├── worker/             # Task execution engine, executor factory (HTTP, Webhook, Internal)
│       │   │   ├── kafka/              # Kafka producers, consumers, serializers, retry/DLQ handlers
│       │   │   ├── redis/              # Distributed lock helper, rate limiter, cache keys
│       │   │   ├── exception/          # GlobalExceptionHandler, Custom Domain Exceptions
│       │   │   ├── validation/         # SSRF validator, CronValidator, URLValidator
│       │   │   ├── metrics/            # Micrometer meter registry custom metrics
│       │   │   └── audit/              # Audit logging aspect and event listener
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/       # Flyway V1..V6 SQL migration scripts
│       └── test/
│           ├── java/com/dts/scheduler/ # Unit & Testcontainers Integration tests
│           └── resources/
├── frontend/
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── Dockerfile
│   └── src/
│       ├── components/         # Reusable UI elements (Navbar, Sidebar, Badges, Modals, Timeline)
│       ├── pages/              # Dashboard, TaskList, TaskCreate, TaskDetail, Executions, Workers, Metrics
│       ├── services/           # Axios API client, query hooks, auth store
│       ├── types/              # TypeScript interfaces mirroring backend DTOs
│       └── utils/              # Formatters, cron humanizers, date helpers
├── infrastructure/
│   ├── docker-compose.yml      # Multi-container orchestration (App, Postgres, Redis, Kafka, Prometheus, Grafana)
│   ├── prometheus/
│   │   └── prometheus.yml
│   └── grafana/
│       ├── provisioning/
│       └── dashboards/
│           └── dts-metrics.json
├── docs/                       # Comprehensive architecture, database, kafka, security & interview specs
├── .github/
│   └── workflows/
│       └── ci-cd.yml           # GitHub Actions pipeline
├── .env.example
├── README.md
└── LICENSE
```
