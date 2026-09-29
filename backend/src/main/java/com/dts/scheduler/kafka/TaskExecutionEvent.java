package com.dts.scheduler.kafka;

import com.dts.scheduler.entity.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionEvent implements Serializable {
    private Long taskId;
    private String executionId;
    private String taskName;
    private TaskType taskType;
    private String payload;
    private int timeoutSeconds;
    private int attempt;
    private int maxRetries;
    private int retryDelaySeconds;
    private double backoffMultiplier;
    private int maxRetryDelaySeconds;
    private Instant scheduledAt;
    private String correlationId;
}
