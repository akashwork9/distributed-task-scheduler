package com.dts.scheduler.dto.task;

import com.dts.scheduler.entity.ConcurrencyPolicy;
import com.dts.scheduler.entity.RetryPolicy;
import com.dts.scheduler.entity.ScheduleType;
import com.dts.scheduler.entity.TaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    @Size(min = 2, max = 255, message = "Task name must be between 2 and 255 characters")
    private String name;

    private String description;

    private TaskType taskType;

    private String payload;

    private ScheduleType scheduleType;

    private String cronExpression;

    private String timezone;

    private Instant scheduledAt;

    @Min(value = 1, message = "Interval must be at least 1 second")
    private Long intervalSeconds;

    private ConcurrencyPolicy concurrencyPolicy;

    private RetryPolicy retryPolicy;

    @Min(value = 0, message = "Max retries must be >= 0")
    private Integer maxRetries;

    @Min(value = 1, message = "Retry delay must be >= 1 second")
    private Integer retryDelaySeconds;

    private Double backoffMultiplier;

    private Integer maxRetryDelaySeconds;

    @Min(value = 1, message = "Timeout must be >= 1 second")
    private Integer timeoutSeconds;

    private Boolean enabled;
}
