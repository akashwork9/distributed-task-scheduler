# Horizontal Scalability & Performance Engineering

---

## 1. Multi-Scheduler and Multi-Worker Scaling

The DTS platform is designed for linear horizontal scalability across all tiers:

```mermaid
flowchart TB
    subgraph Schedulers["Distributed Schedulers (N Instances)"]
        S1["Scheduler 1 (Holder of Redis Scan Lock)"]
        S2["Scheduler 2 (Standby Poller)"]
        S3["Scheduler 3 (Standby Poller)"]
    end

    subgraph KafkaStream["Kafka Topic: task.execution.requested"]
        P0["Partition 0"]
        P1["Partition 1"]
        P2["Partition 2"]
    end

    subgraph WorkerPool["Worker Fleet (Consumer Group: dts-worker-group)"]
        W1["Worker 1 (Processes Partition 0)"]
        W2["Worker 2 (Processes Partition 1)"]
        W3["Worker 3 (Processes Partition 2)"]
    end

    S1 --> KafkaStream
    KafkaStream --> WorkerPool
```

### Running Multi-Node Scaled Clusters Locally
```bash
# Launch DTS with 3 workers and 3 schedulers simultaneously
docker compose up --scale backend=3
```

---

## 2. Why Horizontal Scaling Does Not Cause Duplicate Scheduling
1. **Coarse-Grained Redis Mutual Exclusion**:
   Every polling cycle, all scheduler nodes attempt to acquire the lock `lock:scheduler:poll`. Exactly one instance wins the lock; the remaining $(N-1)$ instances yield immediately.
2. **Fine-Grained PostgreSQL Row Partitioning**:
   The winning scheduler executes:
   ```sql
   SELECT * FROM tasks 
   WHERE status = 'ACTIVE' AND enabled = true AND next_run_at <= :now
   ORDER BY next_run_at ASC 
   LIMIT :batchSize
   FOR UPDATE SKIP LOCKED;
   ```
   Even if multiple threads execute the query concurrently, `SKIP LOCKED` guarantees that each thread receives a mutually exclusive set of task rows without locking conflicts or blocked connections.
3. **Partitioned Kafka Dispatch**:
   The scheduler partitions outgoing messages by `taskId`. All executions of a given task flow to the same Kafka partition, preserving sequential ordering when `FORBID_CONCURRENT` is enforced.
4. **Worker Parallelism**:
   Kafka consumer group rebalancing dynamically assigns partitions to active workers. If a worker fails or is terminated, Kafka automatically triggers a rebalance and assigns its partition to surviving worker nodes within seconds.
