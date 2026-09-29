package com.dts.scheduler.dto.execution;

import com.dts.scheduler.entity.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionResponse {
    private Long id;
    private Long taskId;
    private String taskName;
    private String executionId;
    private ExecutionStatus status;
    private int attempt;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private String workerId;
    private String errorMessage;
    private String result;
    private Instant createdAt;
}
