# Database Design Specification

---

## 1. Relational Model & Entity Relationships

The Distributed Task Scheduler database is designed for PostgreSQL 15+ using strong relational integrity, primary keys (`BIGSERIAL`), foreign keys with explicit cascade/set-null policies, unique constraints for idempotency, and compound B-tree indexes optimized for the high-frequency polling queries.

```mermaid
erDiagram
    USERS ||--o{ TASKS : owns
    USERS ||--o{ AUDIT_LOGS : records
    TASKS ||--o{ TASK_EXECUTIONS : spawns

    USERS {
        bigserial id PK
        varchar email UK
        varchar password_hash
        varchar name
        varchar role
        timestamptz created_at
        timestamptz updated_at
    }

    TASKS {
        bigserial id PK
        bigint user_id FK
        varchar name
        text description
        varchar task_type
        text payload
        varchar schedule_type
        varchar cron_expression
        varchar timezone
        timestamptz scheduled_at
        bigint interval_seconds
        timestamptz next_run_at
        timestamptz last_run_at
        varchar status
        varchar concurrency_policy
        varchar retry_policy
        int max_retries
        int retry_delay_seconds
        double_precision backoff_multiplier
        int max_retry_delay_seconds
        int timeout_seconds
        boolean enabled
        bigint version
        timestamptz created_at
        timestamptz updated_at
    }

    TASK_EXECUTIONS {
        bigserial id PK
        bigint task_id FK
        varchar execution_id UK
        varchar status
        int attempt
        timestamptz scheduled_at
        timestamptz started_at
        timestamptz completed_at
        bigint duration_ms
        varchar worker_id
        text error_message
        text result
        bigint version
        timestamptz created_at
        timestamptz updated_at
    }

    WORKERS {
        bigserial id PK
        varchar worker_id UK
        varchar hostname
        varchar ip_address
        varchar status
        timestamptz last_heartbeat
        int active_jobs
        int capacity
        timestamptz registered_at
        timestamptz updated_at
    }

    AUDIT_LOGS {
        bigserial id PK
        bigint user_id FK
        varchar action
        varchar entity_type
        varchar entity_id
        text metadata
        varchar ip_address
        timestamptz created_at
    }
```

---

## 2. Table Indexing & Query Optimization Rationale

### 2.1 The Critical Poller Index
```sql
CREATE INDEX idx_tasks_schedule_poll ON tasks (status, enabled, next_run_at);
```
* **Query Pattern**:
  ```sql
  SELECT * FROM tasks
  WHERE status = 'ACTIVE' AND enabled = true AND next_run_at <= :now
  ORDER BY next_run_at ASC
  LIMIT :batchSize
  FOR UPDATE SKIP LOCKED;
  ```
* **Performance Impact**: An index scan across `(status, enabled, next_run_at)` avoids expensive table sequential scans even when `tasks` contains millions of records.
* **Row-Level Concurrency**: `FOR UPDATE SKIP LOCKED` allows parallel scheduler threads to pick distinct batches without lock contention.

### 2.2 Execution Deduplication Index
```sql
CONSTRAINT uk_task_executions_execution_id UNIQUE (execution_id);
```
* Guarantees at the storage engine level that duplicate Kafka deliveries cannot create redundant execution records.
* Allows optimistic claiming:
  ```sql
  UPDATE task_executions
  SET status = 'RUNNING', started_at = CURRENT_TIMESTAMP, worker_id = :workerId
  WHERE execution_id = :executionId AND status = 'QUEUED';
  ```

### 2.3 Optimistic Locking (`version` Column)
* Both `tasks` and `task_executions` include `version BIGINT NOT NULL DEFAULT 0`.
* Annotated with `@Version` in Spring Data JPA.
* Eliminates the lost-update anomaly if an API user modifies task parameters concurrently with the scheduler advancing `next_run_at`.
