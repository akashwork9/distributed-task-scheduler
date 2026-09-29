package com.dts.scheduler.service;

import com.dts.scheduler.dto.PageResponse;
import com.dts.scheduler.dto.execution.TaskExecutionResponse;
import com.dts.scheduler.entity.ExecutionStatus;
import com.dts.scheduler.entity.Task;
import com.dts.scheduler.entity.TaskExecution;
import com.dts.scheduler.entity.User;
import com.dts.scheduler.exception.BadRequestException;
import com.dts.scheduler.exception.ResourceNotFoundException;
import com.dts.scheduler.exception.UnauthorizedException;
import com.dts.scheduler.kafka.TaskEventProducer;
import com.dts.scheduler.kafka.TaskExecutionEvent;
import com.dts.scheduler.repository.TaskExecutionRepository;
import com.dts.scheduler.repository.TaskRepository;
import com.dts.scheduler.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskRepository taskRepository;
    private final TaskEventProducer taskEventProducer;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public TaskExecutionResponse getExecutionById(String idOrExecutionId) {
        TaskExecution execution;
        try {
            Long numericId = Long.parseLong(idOrExecutionId);
            execution = taskExecutionRepository.findByIdWithTask(numericId)
                    .or(() -> taskExecutionRepository.findByExecutionIdWithTask(idOrExecutionId))
                    .orElseThrow(() -> new ResourceNotFoundException("Task execution not found with id: " + idOrExecutionId));
        } catch (NumberFormatException e) {
            execution = taskExecutionRepository.findByExecutionIdWithTask(idOrExecutionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task execution not found with executionId: " + idOrExecutionId));
        }

        validateOwnership(execution.getTask());
        return mapToExecutionResponse(execution);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskExecutionResponse> getExecutionsByTaskId(Long taskId, Pageable pageable) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        validateOwnership(task);
        Page<TaskExecution> page = taskExecutionRepository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable);
        return PageResponse.of(page.map(this::mapToExecutionResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskExecutionResponse> getAllExecutions(Pageable pageable) {
        Page<TaskExecution> page = taskExecutionRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.of(page.map(this::mapToExecutionResponse));
    }

    @Transactional
    public TaskExecutionResponse retryExecution(Long id) {
        TaskExecution execution = taskExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task execution not found with id: " + id));

        Task task = execution.getTask();
        validateOwnership(task);

        if (execution.getStatus() != ExecutionStatus.FAILED && execution.getStatus() != ExecutionStatus.CANCELLED) {
            throw new BadRequestException("Only FAILED or CANCELLED executions can be retried. Current status: " + execution.getStatus());
        }

        Instant now = Instant.now();
        String newExecutionId = UUID.randomUUID().toString();

        TaskExecution retryExecution = TaskExecution.builder()
                .task(task)
                .executionId(newExecutionId)
                .status(ExecutionStatus.QUEUED)
                .attempt(1)
                .scheduledAt(now)
                .build();

        retryExecution = taskExecutionRepository.save(retryExecution);

        TaskExecutionEvent event = TaskExecutionEvent.builder()
                .taskId(task.getId())
                .executionId(newExecutionId)
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
                .correlationId(UUID.randomUUID().toString())
                .build();

        taskEventProducer.publishTaskRequested(event);
        auditService.record(SecurityUtils.getCurrentUser(), "EXECUTION_MANUAL_RETRY", "TaskExecution",
                String.valueOf(retryExecution.getId()), "Retried failed execution originalId=" + execution.getExecutionId());

        return mapToExecutionResponse(retryExecution);
    }

    private void validateOwnership(Task task) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (!SecurityUtils.isAdmin() && !task.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to view or retry this execution");
        }
    }

    public TaskExecutionResponse mapToExecutionResponse(TaskExecution execution) {
        return TaskExecutionResponse.builder()
                .id(execution.getId())
                .taskId(execution.getTask().getId())
                .taskName(execution.getTask().getName())
                .executionId(execution.getExecutionId())
                .status(execution.getStatus())
                .attempt(execution.getAttempt())
                .scheduledAt(execution.getScheduledAt())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .durationMs(execution.getDurationMs())
                .workerId(execution.getWorkerId())
                .errorMessage(execution.getErrorMessage())
                .result(execution.getResult())
                .createdAt(execution.getCreatedAt())
                .build();
    }
}
