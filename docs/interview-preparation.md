# DTS System Design & Software Engineering Interview Guide

---

## 1. Core Distributed Systems Concepts

### Q1: Why can't Kafka alone guarantee "exactly-once" execution in this architecture?
**Answer**:
"Kafka provides an exactly-once semantics (EOS) guarantee within its own closed ecosystem (Kafka-to-Kafka streaming using transactional producers and consumer offsets). However, our task scheduler coordinates with heterogeneous systems outside Kafka: an external third-party HTTP endpoint and an external PostgreSQL database. 

If a worker successfully calls the external HTTP endpoint, but crashes before acknowledging the Kafka offset or committing the database transaction, Kafka will inevitably re-deliver that message to another worker during rebalance (**At-Least-Once delivery**). 

Therefore, true end-to-end exactly-once is impossible without **consumer-side idempotency**. In our architecture, the scheduler generates a globally unique `executionId` stored with a `UNIQUE` database constraint. The worker executes an atomic conditional update:
```sql
UPDATE task_executions 
SET status = 'RUNNING', started_at = NOW(), worker_id = :workerId
WHERE execution_id = :executionId AND status = 'QUEUED';
```
If 0 rows are affected, the message is recognized as a duplicate and safely discarded without invoking duplicate side-effects."

---

### Q2: Why combine Redis distributed locking with PostgreSQL `FOR UPDATE SKIP LOCKED`?
**Answer**:
"We employ a two-tier coordination pattern:
1. **Tier 1 (Redis Coarse Lock via Redisson / SETNX)**:
   In a cluster with 10 scheduler instances running every 5 seconds, allowing all 10 nodes to run full table scans on PostgreSQL creates connection pool saturation and index contention. Redis acts as a fast leaderless coordinator: only one scheduler acquires `lock:scheduler:poll` to perform the poll.
2. **Tier 2 (PostgreSQL Fine-Grained `SKIP LOCKED`)**:
   Distributed locks in Redis can experience lease expiration during network partitions or GC pauses. By having the winning scheduler execute `SELECT ... FOR UPDATE SKIP LOCKED`, PostgreSQL guarantees at the ACID storage engine level that two threads or nodes can never lock or dispatch the same task row simultaneously, completely eliminating race conditions."

---

### Q3: How does the system handle a worker node crashing while executing a 10-minute task?
**Answer**:
"Workers send periodic heartbeats every 10 seconds to the `workers` table and Redis. We implement an automated `ZombieTaskRecoveryDaemon` running on the scheduler:
1. It queries `task_executions` for jobs in `RUNNING` status whose `startedAt` exceeds `timeoutSeconds + 60s` grace period, or whose assigned worker node has stopped sending heartbeats.
2. If the task has retries remaining (`attempt < maxRetries`), the daemon transitions the state to `RETRYING`, calculates the exponential backoff delay, and republishes the event to `task.execution.retry`.
3. If retries are exhausted, it marks the execution as `FAILED` with an error message indicating worker abandonment, and routes the event to `task.execution.dlq` for administrator inspection."

---

## 2. Spring Boot, Java & Database Engineering

### Q4: How is optimistic locking used and why not pessimistic locking for task updates?
**Answer**:
"In the `tasks` and `task_executions` entities, we utilize JPA `@Version private Long version;`. 

Pessimistic locking (`SELECT FOR UPDATE`) holds database locks for the entire duration of a transaction, reducing throughput and introducing deadlock risks when multiple users or daemons touch tasks.

Optimistic locking avoids holding locks. When an API user updates task parameters at the exact millisecond the scheduler poller advances `nextRunAt`, Hibernate verifies `WHERE id = :id AND version = :version`. If a collision occurs, Hibernate throws an `OptimisticLockingFailureException`, which our `GlobalExceptionHandler` converts to a clean `HTTP 409 Conflict`, allowing the client to refresh and retry."

---

### Q5: How is SSRF prevented for user-supplied HTTP task targets?
**Answer**:
"Allowing users to specify arbitrary HTTP URLs exposes internal cloud infrastructure to Server-Side Request Forgery.
Our `SsrfValidator`:
1. Enforces HTTP/HTTPS protocol schemes (rejecting `file://`, `ftp://`).
2. Resolves DNS via `InetAddress.getAllByName(host)` before opening sockets.
3. Iterates over all resolved IPs to verify they do not belong to loopback (`127.0.0.1/8`, `::1`), cloud link-local metadata addresses (`169.254.169.254`), or private RFC 1918 subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
4. Explicitly configures the HTTP client with `followRedirects(HttpClient.Redirect.NEVER)` to prevent redirect bypasses."

---

## 3. High-Scale System Design Interview

> **"Design a distributed task scheduler supporting 10,000,000 scheduled jobs."**

### How This Implementation Evolves to 10M Tasks:
1. **Database Sharding & Time-Bucket Partitioning**:
   * Instead of querying a single PostgreSQL table, partition the `tasks` table by time buckets (e.g. `next_run_at` partitioned into 1-hour or 15-minute table partitions). Dropping old history becomes $O(1)$ partition drops.
2. **Distributed Redis Hierarchical Delay Wheel / Sorted Set (ZSET)**:
   * Tasks due in the next 15 minutes are staged from PostgreSQL into a Redis ZSET where `score = next_run_at_epoch_ms` and `member = taskId`.
   * High-throughput pollers fetch tasks using `ZRANGEBYSCORE key 0 :now_ms LIMIT :batch` ($O(\log N + M)$ complexity).
3. **Kafka Partition Scaling**:
   * Scale `task.execution.requested` to 64 or 128 partitions.
   * Employ Kafka consumer groups with hundreds of containerized worker replicas running on Kubernetes with Horizontal Pod Autoscaler (HPA) triggered by Kafka consumer lag metrics.
4. **Execution History Cold Storage**:
   * Move completed execution logs from PostgreSQL to ClickHouse, Amazon S3 / Snowflake for cost-effective long-term audit analytics.
