package com.dts.scheduler.kafka;

import com.dts.scheduler.entity.ExecutionStatus;
import com.dts.scheduler.entity.TaskExecution;
import com.dts.scheduler.repository.TaskExecutionRepository;
import com.dts.scheduler.worker.TaskExecutionResult;
import com.dts.scheduler.worker.TaskExecutor;
import com.dts.scheduler.worker.TaskExecutorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExecutionConsumer {

    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskExecutorFactory taskExecutorFactory;
    private final TaskEventProducer taskEventProducer;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${dts.worker.id:worker-node-1}")
    private String workerId;

    @Value("${dts.worker.enabled:true}")
    private boolean workerEnabled;

    @KafkaListener(
            topics = {"${dts.kafka.topics.task-requested:task.execution.requested}", "${dts.kafka.topics.task-retry:task.execution.retry}"},
            groupId = "dts-worker-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTaskExecution(
            @Payload String payloadString,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        if (!workerEnabled) {
            log.trace("Worker processing disabled on this node; acknowledging and passing.");
            acknowledgment.acknowledge();
            return;
        }

        TaskExecutionEvent event;
        try {
            event = objectMapper.readValue(payloadString, TaskExecutionEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize task execution event payload: {}", payloadString, e);
            acknowledgment.acknowledge();
            return;
        }

        log.info("Worker [{}] consumed execution event: executionId={} taskId={} attempt={}/{} topic={}",
                workerId, event.getExecutionId(), event.getTaskId(), event.getAttempt(), event.getMaxRetries(), topic);

        try {
            processExecutionWithIdempotency(event);
        } catch (Exception e) {
            log.error("Unhandled error processing executionId={}: {}", event.getExecutionId(), e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    public void processExecutionWithIdempotency(TaskExecutionEvent event) {
        String executionId = event.getExecutionId();
        Instant now = Instant.now();

        // 1. Transaction 1: Atomically claim & transition to RUNNING
        Optional<TaskExecution> optionalExecution = taskExecutionRepository.findByExecutionIdWithTask(executionId);
        if (optionalExecution.isEmpty()) {
            log.warn("Execution record [{}] not found in database. Discarding message.", executionId);
            return;
        }

        TaskExecution execution = optionalExecution.get();

        // If execution has already concluded, ignore duplicate
        if (execution.getStatus() == ExecutionStatus.SUCCESS || execution.getStatus() == ExecutionStatus.FAILED) {
            log.info("Idempotent check: Execution [{}] is already in terminal state [{}]. Discarding duplicate.",
                    executionId, execution.getStatus());
            return;
        }

        // Atomically transition from QUEUED/RETRYING to RUNNING
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setWorkerId(workerId);
        execution.setStartedAt(now);
        execution.setAttempt(event.getAttempt());
        taskExecutionRepository.saveAndFlush(execution);

        // 2. Perform external network/worker execution OUTSIDE database transaction
        TaskExecutor executor = taskExecutorFactory.getExecutor(event.getTaskType());
        long startNanos = System.nanoTime();
        TaskExecutionResult result = executor.execute(event);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        Timer.builder("dts_task_execution_duration")
                .tag("type", event.getTaskType().name())
                .tag("status", result.isSuccess() ? "SUCCESS" : "FAILED")
                .register(meterRegistry)
                .record(elapsedMillis, TimeUnit.MILLISECONDS);

        // 3. Transaction 2: Record completion state
        if (result.isSuccess()) {
            handleExecutionSuccess(executionId, result, event);
        } else {
            handleExecutionFailure(executionId, result, event);
        }
    }

    @Transactional
    public void handleExecutionSuccess(String executionId, TaskExecutionResult result, TaskExecutionEvent event) {
        Optional<TaskExecution> optional = taskExecutionRepository.findByExecutionIdWithTask(executionId);
        if (optional.isEmpty()) return;
        TaskExecution execution = optional.get();

        Instant completedAt = Instant.now();
        execution.setStatus(ExecutionStatus.SUCCESS);
        execution.setCompletedAt(completedAt);
        execution.setDurationMs(result.getDurationMs());
        execution.setResult(result.getResultData());
        execution.setErrorMessage(null);
        taskExecutionRepository.save(execution);

        taskEventProducer.publishTaskCompleted(event);
        meterRegistry.counter("dts_tasks_executed_success", "type", event.getTaskType().name()).increment();
        log.info("Execution [{}] succeeded in {}ms", execution.getExecutionId(), result.getDurationMs());
    }

    @Transactional
    public void handleExecutionFailure(String executionId, TaskExecutionResult result, TaskExecutionEvent event) {
        Optional<TaskExecution> optional = taskExecutionRepository.findByExecutionIdWithTask(executionId);
        if (optional.isEmpty()) return;
        TaskExecution execution = optional.get();

        int currentAttempt = event.getAttempt();
        int maxRetries = event.getMaxRetries();

        execution.setDurationMs(result.getDurationMs());
        execution.setErrorMessage(result.getErrorMessage());

        if (currentAttempt < maxRetries) {
            long delaySeconds = (long) (event.getRetryDelaySeconds() * Math.pow(event.getBackoffMultiplier(), currentAttempt - 1));
            delaySeconds = Math.min(delaySeconds, event.getMaxRetryDelaySeconds());

            execution.setStatus(ExecutionStatus.RETRYING);
            taskExecutionRepository.save(execution);

            event.setAttempt(currentAttempt + 1);
            taskEventProducer.publishTaskRetry(event);
            meterRegistry.counter("dts_tasks_retried_total", "type", event.getTaskType().name()).increment();

            log.warn("Execution [{}] failed (attempt {}/{}). Retrying with backoff {}s: {}",
                    execution.getExecutionId(), currentAttempt, maxRetries, delaySeconds, result.getErrorMessage());
        } else {
            Instant completedAt = Instant.now();
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setCompletedAt(completedAt);
            taskExecutionRepository.save(execution);

            taskEventProducer.publishTaskDlq(event);
            meterRegistry.counter("dts_tasks_dlq_total", "type", event.getTaskType().name()).increment();

            log.error("Execution [{}] exhausted retries ({}/{}). Routed to DLQ. Error: {}",
                    execution.getExecutionId(), currentAttempt, maxRetries, result.getErrorMessage());
        }
    }
}
