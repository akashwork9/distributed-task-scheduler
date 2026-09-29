package com.dts.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 50)
    private TaskType taskType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 50)
    private ScheduleType scheduleType;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "interval_seconds")
    private Long intervalSeconds;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TaskStatus status = TaskStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "concurrency_policy", nullable = false, length = 50)
    @Builder.Default
    private ConcurrencyPolicy concurrencyPolicy = ConcurrencyPolicy.ALLOW_CONCURRENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_policy", nullable = false, length = 50)
    @Builder.Default
    private RetryPolicy retryPolicy = RetryPolicy.EXPONENTIAL_BACKOFF;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    @Column(name = "retry_delay_seconds", nullable = false)
    @Builder.Default
    private int retryDelaySeconds = 10;

    @Column(name = "backoff_multiplier", nullable = false)
    @Builder.Default
    private double backoffMultiplier = 2.0;

    @Column(name = "max_retry_delay_seconds", nullable = false)
    @Builder.Default
    private int maxRetryDelaySeconds = 300;

    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private int timeoutSeconds = 30;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
