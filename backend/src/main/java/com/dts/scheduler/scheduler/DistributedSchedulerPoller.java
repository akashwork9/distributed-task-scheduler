package com.dts.scheduler.scheduler;

import com.dts.scheduler.entity.*;
import com.dts.scheduler.kafka.TaskEventProducer;
import com.dts.scheduler.kafka.TaskExecutionEvent;
import com.dts.scheduler.redis.DistributedLockService;
import com.dts.scheduler.repository.TaskExecutionRepository;
import com.dts.scheduler.repository.TaskRepository;
import com.dts.scheduler.service.AuditService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedSchedulerPoller {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final DistributedLockService lockService;
    private final TaskEventProducer taskEventProducer;
    private final ScheduleCalculator scheduleCalculator;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    @Value("${dts.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${dts.scheduler.batch-size:50}")
    private int batchSize;

    @Value("${dts.scheduler.lock-name:lock:scheduler:poll}")
    private String lockName;

    @Value("${dts.scheduler.lock-wait-time-ms:2000}")
    private long lockWaitTimeMs;

    @Value("${dts.scheduler.lock-lease-time-ms:10000}")
    private long lockLeaseTimeMs;

    @Scheduled(fixedDelayString = "${dts.scheduler.poll-interval-ms:5000}")
    public void pollAndSchedule() {
        if (!schedulerEnabled) {
            return;
        }

        boolean lockAcquired = lockService.executeWithLock(
                lockName,
                lockWaitTimeMs,
                lockLeaseTimeMs,
                this::processEligibleTasksBatch
        );

        if (!lockAcquired) {
            log.trace("Scheduler poll lock held by peer instance; skipping scan cycle.");
        }
    }

    @Transactional
    public void processEligibleTasksBatch() {
        Instant now = Instant.now();
        List<Task> eligibleTasks = taskRepository.findEligibleTasksForScheduling(now, batchSize);

        if (eligibleTasks.isEmpty()) {
            return;
        }

        log.info("Distributed Scheduler claimed batch of {} eligible tasks for scheduling", eligibleTasks.size());

        for (Task task : eligibleTasks) {
            try {
                processSingleTask(task, now);
            } catch (Exception e) {
                log.error("Failed to schedule task id={}: {}", task.getId(), e.getMessage(), e);
            }
        }
    }

    private void processSingleTask(Task task, Instant now) {
        // Concurrency Check
        if (task.getConcurrencyPolicy() == ConcurrencyPolicy.FORBID_CONCURRENT
                && taskExecutionRepository.hasActiveExecution(task.getId())) {
            log.warn("Task id={} [{}] has FORBID_CONCURRENT policy and is currently running. Skipping execution cycle.",
                    task.getId(), task.getName());

            Instant nextRun = scheduleCalculator.calculateNextRunAt(task, now);
            task.setNextRunAt(nextRun);
            if (task.getScheduleType() == ScheduleType.ONE_TIME) {
                task.setStatus(TaskStatus.COMPLETED);
                task.setEnabled(false);
            }
            taskRepository.save(task);

            auditService.record(task.getUser(), "TASK_CONCURRENCY_SKIPPED", "Task",
                    String.valueOf(task.getId()), "Skipped run due to active overlapping execution");
            return;
        }

        // Generate unique idempotency key
        String executionId = UUID.randomUUID().toString();

        TaskExecution execution = TaskExecution.builder()
                .task(task)
                .executionId(executionId)
                .status(ExecutionStatus.QUEUED)
                .attempt(1)
                .scheduledAt(task.getNextRunAt() != null ? task.getNextRunAt() : now)
                .build();

        taskExecutionRepository.save(execution);

        // Advance task next run time
        task.setLastRunAt(now);
        if (task.getScheduleType() == ScheduleType.ONE_TIME) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setEnabled(false);
            task.setNextRunAt(null);
        } else {
            task.setNextRunAt(scheduleCalculator.calculateNextRunAt(task, now));
        }
        taskRepository.save(task);

        // Publish to Kafka
        TaskExecutionEvent event = TaskExecutionEvent.builder()
                .taskId(task.getId())
                .executionId(executionId)
                .taskName(task.getName())
                .taskType(task.getTaskType())
                .payload(task.getPayload())
                .timeoutSeconds(task.getTimeoutSeconds())
                .attempt(1)
                .maxRetries(task.getMaxRetries())
                .retryDelaySeconds(task.getRetryDelaySeconds())
                .backoffMultiplier(task.getBackoffMultiplier())
                .maxRetryDelaySeconds(task.getMaxRetryDelaySeconds())
                .scheduledAt(execution.getScheduledAt())
                .correlationId(UUID.randomUUID().toString())
                .build();

        taskEventProducer.publishTaskRequested(event);
        meterRegistry.counter("dts_tasks_scheduled_total", "type", task.getTaskType().name()).increment();
    }
}
