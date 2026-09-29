package com.dts.scheduler.dto.task;

import com.dts.scheduler.entity.ConcurrencyPolicy;
import com.dts.scheduler.entity.RetryPolicy;
import com.dts.scheduler.entity.ScheduleType;
import com.dts.scheduler.entity.TaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateTaskRequest {

    @NotBlank(message = "Task name is required")
    @Size(min = 2, max = 255, message = "Task name must be between 2 and 255 characters")
    private String name;

    private String description;

    @NotNull(message = "Task type is required (HTTP_TASK, WEBHOOK_TASK, INTERNAL_TASK)")
    private TaskType taskType;

    @NotBlank(message = "Payload is required (JSON format for HTTP/Webhook or task identifier)")
    private String payload;

    @NotNull(message = "Schedule type is required (ONE_TIME, INTERVAL, CRON)")
    private ScheduleType scheduleType;

    private String cronExpression;

    @Builder.Default
    private String timezone = "UTC";

    private Instant scheduledAt;

    @Min(value = 1, message = "Interval must be at least 1 second")
    private Long intervalSeconds;

    @Builder.Default
    private ConcurrencyPolicy concurrencyPolicy = ConcurrencyPolicy.ALLOW_CONCURRENT;

    @Builder.Default
    private RetryPolicy retryPolicy = RetryPolicy.EXPONENTIAL_BACKOFF;

    @Min(value = 0, message = "Max retries must be >= 0")
    @Builder.Default
    private int maxRetries = 3;

    @Min(value = 1, message = "Retry delay must be >= 1 second")
    @Builder.Default
    private int retryDelaySeconds = 10;

    @Builder.Default
    private double backoffMultiplier = 2.0;

    @Builder.Default
    private int maxRetryDelaySeconds = 300;

    @Min(value = 1, message = "Timeout must be >= 1 second")
    @Builder.Default
    private int timeoutSeconds = 30;

    @Builder.Default
    private boolean enabled = true;
}
