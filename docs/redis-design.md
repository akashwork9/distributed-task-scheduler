# Redis Coordination, Distributed Locking & Rate Limiting

---

## 1. Role of Redis in Distributed Task Scheduler

Redis serves three mission-critical functions in DTS:
1. **Distributed Mutual Exclusion (Locks)**: Prevents multiple scheduler replicas from scanning and double-dispatching the same task batches.
2. **High-Speed API Rate Limiting**: Protects backend APIs against abusive requests.
3. **Cluster Node Heartbeat Tracking**: Maintains worker node liveness with millisecond latency.

---

## 2. Distributed Locking Protocol: Redisson & SETNX

```mermaid
sequenceDiagram
    participant Sched1 as Scheduler Node 1
    participant Sched2 as Scheduler Node 2
    participant Redis as Redis Cluster (Redisson)
    participant Postgres as PostgreSQL ACID

    Sched1->>Redis: tryLock("lock:scheduler:poll", wait=2s, lease=10s)
    Redis-->>Sched1: Lock Acquired (Token generated)
    Sched2->>Redis: tryLock("lock:scheduler:poll", wait=2s, lease=10s)
    Redis-->>Sched2: Lock Busy (Contention rejected)

    Note over Sched2: Yields execution cycle; sleeps until next interval

    Sched1->>Postgres: SELECT * FROM tasks WHERE status='ACTIVE' AND next_run_at <= NOW() FOR UPDATE SKIP LOCKED
    Sched1->>Postgres: INSERT task_executions (QUEUED) & UPDATE tasks
    Sched1->>Redis: unlock("lock:scheduler:poll")
    Redis-->>Sched1: Released
```

### 2.1 Why Primitive In-Memory Locks Fail
In-memory Java locks (`synchronized`, `ReentrantLock`) only synchronize threads within a single JVM instance. In a cloud deployment with 3+ containerized scheduler replicas, in-memory locks provide zero coordination.

### 2.2 SETNX and Expiration Semantics
* **Atomic Claim**: `SET lock:scheduler:poll <uuid> NX PX 10000`
  * `NX`: Only set if key does not already exist.
  * `PX 10000`: Set automatic expiration of 10,000 milliseconds (10 seconds).
* **Deadlock Prevention**: Even if a scheduler JVM experiences an ungraceful shutdown (`kill -9`, hardware loss), the lock expires automatically after 10 seconds, allowing peer schedulers to proceed.
* **Redisson Watchdog**: For long-running operations, Redisson's background watchdog thread periodically renews the lease TTL until the transaction completes, preventing premature lock release.

---

## 3. Distributed API Rate Limiting

### Algorithm: Atomic Increment with Window Expiration
* Target: 120 requests/minute per client IP / user.
* Implementation:
  ```java
  Long count = stringRedisTemplate.opsForValue().increment("rate_limit:" + clientIp);
  if (count == 1) {
      stringRedisTemplate.expire("rate_limit:" + clientIp, 60, TimeUnit.SECONDS);
  }
  if (count > 120) {
      throw new RateLimitExceededException("Rate limit exceeded");
  }
  ```
* Time Complexity: $O(1)$ operations with zero locks.
