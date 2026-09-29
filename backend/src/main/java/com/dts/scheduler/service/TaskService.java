package com.dts.scheduler.service;

import com.dts.scheduler.dto.PageResponse;
import com.dts.scheduler.dto.task.CreateTaskRequest;
import com.dts.scheduler.dto.task.HttpPayloadDto;
import com.dts.scheduler.dto.task.TaskResponse;
import com.dts.scheduler.dto.task.UpdateTaskRequest;
import com.dts.scheduler.entity.*;
import com.dts.scheduler.exception.BadRequestException;
import com.dts.scheduler.exception.ResourceNotFoundException;
import com.dts.scheduler.exception.UnauthorizedException;
import com.dts.scheduler.dto.task.TriggerResponse;
import com.dts.scheduler.kafka.TaskEventProducer;
import com.dts.scheduler.kafka.TaskExecutionEvent;
import com.dts.scheduler.repository.TaskExecutionRepository;
import com.dts.scheduler.repository.TaskRepository;
import com.dts.scheduler.scheduler.ScheduleCalculator;
import com.dts.scheduler.security.SecurityUtils;
import com.dts.scheduler.validation.CronExpressionValidator;
import com.dts.scheduler.validation.SsrfValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskEventProducer taskEventProducer;
    private final ScheduleCalculator scheduleCalculator;
    private final SsrfValidator ssrfValidator;
    private final CronExpressionValidator cronValidator;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        validateTaskDefinition(request.getTaskType(), request.getPayload(), request.getScheduleType(), request.getCronExpression(), request.getTimezone());

        Instant nextRunAt = scheduleCalculator.calculateInitialNextRunAt(
                request.getScheduleType(),
                request.getCronExpression(),
                request.getTimezone(),
                request.getScheduledAt(),
                request.getIntervalSeconds()
        );

        Task task = Task.builder()
                .user(currentUser)
                .name(request.getName().trim())
                .description(request.getDescription())
                .taskType(request.getTaskType())
                .payload(request.getPayload())
                .scheduleType(request.getScheduleType())
                .cronExpression(request.getCronExpression())
                .timezone(request.getTimezone() != null ? request.getTimezone().trim() : "UTC")
                .scheduledAt(request.getScheduledAt())
                .intervalSeconds(request.getIntervalSeconds())
                .nextRunAt(nextRunAt)
                .status(request.isEnabled() ? TaskStatus.ACTIVE : TaskStatus.PAUSED)
                .concurrencyPolicy(request.getConcurrencyPolicy())
                .retryPolicy(request.getRetryPolicy())
                .maxRetries(request.getMaxRetries())
                .retryDelaySeconds(request.getRetryDelaySeconds())
                .backoffMultiplier(request.getBackoffMultiplier())
                .maxRetryDelaySeconds(request.getMaxRetryDelaySeconds())
                .timeoutSeconds(request.getTimeoutSeconds())
                .enabled(request.isEnabled())
                .build();

        task = taskRepository.save(task);
        auditService.record(currentUser, "TASK_CREATED", "Task", String.valueOf(task.getId()), "Created task: " + task.getName());
        log.info("Created task id={} for user={}", task.getId(), currentUser.getEmail());

        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = getTaskEntity(id);

        if (request.getName() != null) task.setName(request.getName().trim());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getConcurrencyPolicy() != null) task.setConcurrencyPolicy(request.getConcurrencyPolicy());
        if (request.getRetryPolicy() != null) task.setRetryPolicy(request.getRetryPolicy());
        if (request.getMaxRetries() != null) task.setMaxRetries(request.getMaxRetries());
        if (request.getRetryDelaySeconds() != null) task.setRetryDelaySeconds(request.getRetryDelaySeconds());
        if (request.getBackoffMultiplier() != null) task.setBackoffMultiplier(request.getBackoffMultiplier());
        if (request.getMaxRetryDelaySeconds() != null) task.setMaxRetryDelaySeconds(request.getMaxRetryDelaySeconds());
        if (request.getTimeoutSeconds() != null) task.setTimeoutSeconds(request.getTimeoutSeconds());
        if (request.getEnabled() != null) {
            task.setEnabled(request.getEnabled());
            task.setStatus(request.getEnabled() ? TaskStatus.ACTIVE : TaskStatus.PAUSED);
        }

        // Handle schedule or payload updates
        boolean scheduleChanged = false;
        if (request.getScheduleType() != null) {
            task.setScheduleType(request.getScheduleType());
            scheduleChanged = true;
        }
        if (request.getCronExpression() != null) {
            task.setCronExpression(request.getCronExpression());
            scheduleChanged = true;
        }
        if (request.getTimezone() != null) {
            task.setTimezone(request.getTimezone());
            scheduleChanged = true;
        }
        if (request.getScheduledAt() != null) {
            task.setScheduledAt(request.getScheduledAt());
            scheduleChanged = true;
        }
        if (request.getIntervalSeconds() != null) {
            task.setIntervalSeconds(request.getIntervalSeconds());
            scheduleChanged = true;
        }

        if (request.getPayload() != null || request.getTaskType() != null) {
            if (request.getTaskType() != null) task.setTaskType(request.getTaskType());
            if (request.getPayload() != null) task.setPayload(request.getPayload());
            validateTaskDefinition(task.getTaskType(), task.getPayload(), task.getScheduleType(), task.getCronExpression(), task.getTimezone());
        }

        if (scheduleChanged) {
            validateTaskDefinition(task.getTaskType(), task.getPayload(), task.getScheduleType(), task.getCronExpression(), task.getTimezone());
            Instant nextRunAt = scheduleCalculator.calculateInitialNextRunAt(
                    task.getScheduleType(),
                    task.getCronExpression(),
                    task.getTimezone(),
                    task.getScheduledAt(),
                    task.getIntervalSeconds()
            );
            task.setNextRunAt(nextRunAt);
        }

        task = taskRepository.save(task);
        auditService.record(SecurityUtils.getCurrentUser(), "TASK_UPDATED", "Task", String.valueOf(task.getId()), "Updated task: " + task.getName());
        return mapToTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return mapToTaskResponse(getTaskEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasks(Pageable pageable, TaskStatus status) {
        User currentUser = SecurityUtils.getCurrentUser();
        Page<Task> taskPage;

        if (SecurityUtils.isAdmin()) {
            if (status != null) {
                taskPage = taskRepository.findByStatus(status, pageable);
            } else {
                taskPage = taskRepository.findAll(pageable);
            }
        } else {
            if (status != null) {
                taskPage = taskRepository.findByUserIdAndStatus(currentUser.getId(), status, pageable);
            } else {
                taskPage = taskRepository.findByUserId(currentUser.getId(), pageable);
            }
        }

        return PageResponse.of(taskPage.map(this::mapToTaskResponse));
    }

    @Transactional
    public TaskResponse pauseTask(Long id) {
        Task task = getTaskEntity(id);
        task.setStatus(TaskStatus.PAUSED);
        task.setEnabled(false);
        task = taskRepository.save(task);
        auditService.record(SecurityUtils.getCurrentUser(), "TASK_PAUSED", "Task", String.valueOf(task.getId()), "Paused task");
        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse resumeTask(Long id) {
        Task task = getTaskEntity(id);
        task.setStatus(TaskStatus.ACTIVE);
        task.setEnabled(true);
        if (task.getNextRunAt() == null || task.getNextRunAt().isBefore(Instant.now())) {
            task.setNextRunAt(scheduleCalculator.calculateInitialNextRunAt(
                    task.getScheduleType(),
                    task.getCronExpression(),
                    task.getTimezone(),
                    task.getScheduledAt(),
                    task.getIntervalSeconds()
            ));
        }
        task = taskRepository.save(task);
        auditService.record(SecurityUtils.getCurrentUser(), "TASK_RESUMED", "Task", String.valueOf(task.getId()), "Resumed task");
        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse cancelTask(Long id) {
        Task task = getTaskEntity(id);
        task.setStatus(TaskStatus.CANCELLED);
        task.setEnabled(false);
        task.setNextRunAt(null);
        task = taskRepository.save(task);
        auditService.record(SecurityUtils.getCurrentUser(), "TASK_CANCELLED", "Task", String.valueOf(task.getId()), "Cancelled task");
        return mapToTaskResponse(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskEntity(id);
        taskRepository.delete(task);
        auditService.record(SecurityUtils.getCurrentUser(), "TASK_DELETED", "Task", String.valueOf(id), "Deleted task: " + task.getName());
    }

    @Transactional
    public TriggerResponse triggerTaskImmediately(Long id) {
        Task task = getTaskEntity(id);

        if (task.getConcurrencyPolicy() == ConcurrencyPolicy.FORBID_CONCURRENT
                && taskExecutionRepository.hasActiveExecution(task.getId())) {
            throw new BadRequestException("Task has FORBID_CONCURRENT policy and is currently running an active execution");
        }

        String executionId = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();

        TaskExecution execution = TaskExecution.builder()
                .task(task)
                .executionId(executionId)
                .status(ExecutionStatus.QUEUED)
                .attempt(1)
                .scheduledAt(now)
                .build();

        taskExecutionRepository.save(execution);

        task.setLastRunAt(now);
        taskRepository.save(task);

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
                .scheduledAt(now)
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();

        taskEventProducer.publishTaskRequested(event);
        auditService.record(SecurityUtils.getCurrentUser(), "TASK_MANUALLY_TRIGGERED", "Task",
                String.valueOf(task.getId()), "Manually triggered execution: " + executionId);

        return TriggerResponse.builder()
                .taskId(task.getId())
                .executionId(executionId)
                .status(ExecutionStatus.QUEUED.name())
                .triggeredAt(now)
                .message("Task successfully triggered for immediate execution")
                .build();
    }

    public Task getTaskEntity(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User currentUser = SecurityUtils.getCurrentUser();
        if (!SecurityUtils.isAdmin() && !task.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to access this task");
        }
        return task;
    }

    private void validateTaskDefinition(TaskType taskType, String payload, ScheduleType scheduleType, String cronExpression, String timezone) {
        cronValidator.validateTimezone(timezone);

        if (scheduleType == ScheduleType.CRON) {
            cronValidator.validateCron(cronExpression);
        }

        if (taskType == TaskType.HTTP_TASK || taskType == TaskType.WEBHOOK_TASK) {
            try {
                HttpPayloadDto httpPayload = objectMapper.readValue(payload, HttpPayloadDto.class);
                if (httpPayload.getUrl() == null || httpPayload.getUrl().isBlank()) {
                    throw new BadRequestException("Payload must contain a valid 'url'");
                }
                ssrfValidator.validateSafeUrl(httpPayload.getUrl());
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                // If not valid JSON, check if payload itself is direct URL
                ssrfValidator.validateSafeUrl(payload.trim());
            }
        }
    }

    public TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .userId(task.getUser().getId())
                .userName(task.getUser().getName())
                .name(task.getName())
                .description(task.getDescription())
                .taskType(task.getTaskType())
                .payload(task.getPayload())
                .scheduleType(task.getScheduleType())
                .cronExpression(task.getCronExpression())
                .timezone(task.getTimezone())
                .scheduledAt(task.getScheduledAt())
                .intervalSeconds(task.getIntervalSeconds())
                .nextRunAt(task.getNextRunAt())
                .lastRunAt(task.getLastRunAt())
                .status(task.getStatus())
                .concurrencyPolicy(task.getConcurrencyPolicy())
                .retryPolicy(task.getRetryPolicy())
                .maxRetries(task.getMaxRetries())
                .retryDelaySeconds(task.getRetryDelaySeconds())
                .backoffMultiplier(task.getBackoffMultiplier())
                .maxRetryDelaySeconds(task.getMaxRetryDelaySeconds())
                .timeoutSeconds(task.getTimeoutSeconds())
                .enabled(task.isEnabled())
                .version(task.getVersion())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
