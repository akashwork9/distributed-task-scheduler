package com.dts.scheduler.dto.task;

import com.dts.scheduler.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String name;
    private String description;
    private TaskType taskType;
    private String payload;
    private ScheduleType scheduleType;
    private String cronExpression;
    private String timezone;
    private Instant scheduledAt;
    private Long intervalSeconds;
    private Instant nextRunAt;
    private Instant lastRunAt;
    private TaskStatus status;
    private ConcurrencyPolicy concurrencyPolicy;
    private RetryPolicy retryPolicy;
    private int maxRetries;
    private int retryDelaySeconds;
    private double backoffMultiplier;
    private int maxRetryDelaySeconds;
    private int timeoutSeconds;
    private boolean enabled;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
