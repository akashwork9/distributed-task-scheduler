# Observability, Telemetry & Structured Logging

---

## 1. Metrics & Prometheus Instrumentation

DTS natively integrates **Spring Boot Actuator** and **Micrometer** to expose real-time metrics at `/actuator/prometheus`.

| Metric Name | Type | Description |
| :--- | :---: | :--- |
| `dts_tasks_scheduled_total` | Counter | Total task triggers evaluated and dispatched to Kafka |
| `dts_tasks_executed_success` | Counter | Successful job executions ending in 2xx / valid result |
| `dts_tasks_retried_total` | Counter | Transient execution errors undergoing exponential backoff |
| `dts_tasks_dlq_total` | Counter | Tasks exhausting retry budget and routing to Dead Letter Queue |
| `dts_task_execution_duration` | Timer / Histogram | Latency distribution (p50, p95, p99) of worker executions |
| `dts_zombies_recovered_total` | Counter | Crashed worker jobs reclaimed by the watchdog daemon |
| `dts_dlq_events_received_total`| Counter | Total critical alerts consumed on the DLQ topic |

---

## 2. Structured JSON Logging & Correlation IDs
Every log emitted during task processing includes tracing metadata:
```json
{
  "timestamp": "2026-10-01T12:00:05.123Z",
  "level": "INFO",
  "service": "dts-worker",
  "workerId": "worker-node-1",
  "taskId": 42,
  "executionId": "b8a87b32-9cb4-49c5-8495-35072049d95c",
  "correlationId": "8f307160-5801-4475-bcf2-632514197321",
  "attempt": 1,
  "message": "Execution [b8a87b32-9cb4-49c5-8495-35072049d95c] succeeded in 142ms"
}
```

---

## 3. Health Probes
Exposed at `/actuator/health`:
* **Liveness Probe**: Confirms the JVM process is responsive.
* **Readiness Probe**: Confirms PostgreSQL, Redis, and Kafka cluster connectivity before routing traffic.
