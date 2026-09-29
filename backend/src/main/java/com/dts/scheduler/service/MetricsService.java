package com.dts.scheduler.service;

import com.dts.scheduler.dto.metrics.SystemMetricsResponse;
import com.dts.scheduler.entity.ExecutionStatus;
import com.dts.scheduler.entity.TaskStatus;
import com.dts.scheduler.entity.WorkerStatus;
import com.dts.scheduler.repository.TaskExecutionRepository;
import com.dts.scheduler.repository.TaskRepository;
import com.dts.scheduler.repository.WorkerRepository;
import com.dts.scheduler.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final WorkerRepository workerRepository;

    @Transactional(readOnly = true)
    public SystemMetricsResponse getSystemMetrics() {
        boolean isAdmin = SecurityUtils.isAdmin();
        Long userId = SecurityUtils.getCurrentUserId();

        long totalTasks = isAdmin ? taskRepository.count() : taskRepository.countByUserId(userId);
        long activeTasks = isAdmin ? taskRepository.countByStatus(TaskStatus.ACTIVE) : taskRepository.countByUserIdAndStatus(userId, TaskStatus.ACTIVE);
        long pausedTasks = isAdmin ? taskRepository.countByStatus(TaskStatus.PAUSED) : taskRepository.countByUserIdAndStatus(userId, TaskStatus.PAUSED);

        long totalExecutions = isAdmin ? taskExecutionRepository.count() : taskExecutionRepository.countByUserId(userId);
        long runningExecutions = isAdmin ? taskExecutionRepository.countByStatus(ExecutionStatus.RUNNING) : taskExecutionRepository.countByUserIdAndStatus(userId, ExecutionStatus.RUNNING);
        long successExecutions = isAdmin ? taskExecutionRepository.countByStatus(ExecutionStatus.SUCCESS) : taskExecutionRepository.countByUserIdAndStatus(userId, ExecutionStatus.SUCCESS);
        long failedExecutions = isAdmin ? taskExecutionRepository.countByStatus(ExecutionStatus.FAILED) : taskExecutionRepository.countByUserIdAndStatus(userId, ExecutionStatus.FAILED);
        long retryingExecutions = isAdmin ? taskExecutionRepository.countByStatus(ExecutionStatus.RETRYING) : taskExecutionRepository.countByUserIdAndStatus(userId, ExecutionStatus.RETRYING);

        long activeWorkers = workerRepository.countByStatus(WorkerStatus.ACTIVE);
        long offlineWorkers = workerRepository.countByStatus(WorkerStatus.OFFLINE);

        long completedTotal = successExecutions + failedExecutions;
        double successRate = completedTotal > 0 ? ((double) successExecutions / completedTotal) * 100.0 : 100.0;

        return SystemMetricsResponse.builder()
                .totalTasks(totalTasks)
                .activeTasks(activeTasks)
                .pausedTasks(pausedTasks)
                .totalExecutions(totalExecutions)
                .runningExecutions(runningExecutions)
                .successExecutions(successExecutions)
                .failedExecutions(failedExecutions)
                .retryingExecutions(retryingExecutions)
                .activeWorkers(activeWorkers)
                .offlineWorkers(offlineWorkers)
                .successRatePercentage(Math.round(successRate * 100.0) / 100.0)
                .build();
    }
}
