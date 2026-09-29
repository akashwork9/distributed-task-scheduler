package com.dts.scheduler.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsResponse {
    private long totalTasks;
    private long activeTasks;
    private long pausedTasks;
    private long totalExecutions;
    private long runningExecutions;
    private long successExecutions;
    private long failedExecutions;
    private long retryingExecutions;
    private long activeWorkers;
    private long offlineWorkers;
    private double successRatePercentage;
}
