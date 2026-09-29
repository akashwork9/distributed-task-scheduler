package com.dts.scheduler.scheduler;

import com.dts.scheduler.entity.ExecutionStatus;
import com.dts.scheduler.entity.Task;
import com.dts.scheduler.entity.TaskExecution;
import com.dts.scheduler.entity.Worker;
import com.dts.scheduler.entity.WorkerStatus;
import com.dts.scheduler.kafka.TaskEventProducer;
import com.dts.scheduler.kafka.TaskExecutionEvent;
import com.dts.scheduler.redis.DistributedLockService;
import com.dts.scheduler.repository.TaskExecutionRepository;
import com.dts.scheduler.repository.WorkerRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZombieTaskRecoveryDaemon {

    private final WorkerRepository workerRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final DistributedLockService lockService;
    private final TaskEventProducer taskEventProducer;
    private final MeterRegistry meterRegistry;

    @Value("${dts.worker.zombie-threshold-seconds:60}")
    private long zombieThresholdSeconds;

    @Scheduled(fixedDelay = 30000)
    public void scanAndRecoverZombies() {
        lockService.executeWithLock("lock:scheduler:zombie-recovery", 2000, 25000, this::performRecovery);
    }

    @Transactional
    public void performRecovery() {
        Instant now = Instant.now();
        Instant staleWorkerThreshold = now.minus(Duration.ofSeconds(zombieThresholdSeconds));

        // 1. Mark dead workers as OFFLINE
        List<Worker> staleWorkers = workerRepository.findStaleWorkers(staleWorkerThreshold);
        for (Worker worker : staleWorkers) {
            log.warn("Worker [{}] has not sent heartbeat since {}. Marking OFFLINE.",
                    worker.getWorkerId(), worker.getLastHeartbeat());
            worker.setStatus(WorkerStatus.OFFLINE);
            workerRepository.save(worker);
        }

        // 2. Identify stuck RUNNING task executions (startedAt older than timeout + grace period)
        Instant stuckThreshold = now.minus(Duration.ofSeconds(zombieThresholdSeconds + 30));
        List<TaskExecution> stuckExecutions = taskExecutionRepository.findZombieExecutions(stuckThreshold);

        for (TaskExecution execution : stuckExecutions) {
            Task task = execution.getTask();
            int currentAttempt = execution.getAttempt();
            int maxRetries = task.getMaxRetries();

            log.warn("Zombie execution detected: executionId={} taskId={} attempt={}/{} workerId={}",
                    execution.getExecutionId(), task.getId(), currentAttempt, maxRetries, execution.getWorkerId());

            if (currentAttempt < maxRetries) {
                execution.setStatus(ExecutionStatus.RETRYING);
                execution.setErrorMessage("Worker node crashed or timed out (Reaped by DTS Recovery Daemon)");
                taskExecutionRepository.save(execution);

                TaskExecutionEvent retryEvent = TaskExecutionEvent.builder()
                        .taskId(task.getId())
                        .executionId(execution.getExecutionId())
                        .taskName(task.getName())
                        .taskType(task.getTaskType())
                        .payload(task.getPayload())
                        .timeoutSeconds(task.getTimeoutSeconds())
                        .attempt(currentAttempt + 1)
                        .maxRetries(maxRetries)
                        .retryDelaySeconds(task.getRetryDelaySeconds())
                        .backoffMultiplier(task.getBackoffMultiplier())
                        .maxRetryDelaySeconds(task.getMaxRetryDelaySeconds())
                        .scheduledAt(now)
                        .correlationId(UUID.randomUUID().toString())
                        .build();

                taskEventProducer.publishTaskRetry(retryEvent);
                meterRegistry.counter("dts_zombies_recovered_total").increment();
            } else {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setCompletedAt(now);
                execution.setErrorMessage("Execution timed out and retries exhausted (Reaped by DTS Recovery Daemon)");
                taskExecutionRepository.save(execution);

                TaskExecutionEvent dlqEvent = TaskExecutionEvent.builder()
                        .taskId(task.getId())
                        .executionId(execution.getExecutionId())
                        .taskName(task.getName())
                        .taskType(task.getTaskType())
                        .payload(task.getPayload())
                        .timeoutSeconds(task.getTimeoutSeconds())
                        .attempt(currentAttempt)
                        .maxRetries(maxRetries)
                        .scheduledAt(now)
                        .correlationId(UUID.randomUUID().toString())
                        .build();

                taskEventProducer.publishTaskDlq(dlqEvent);
                meterRegistry.counter("dts_zombies_dlq_total").increment();
            }
        }
    }
}
