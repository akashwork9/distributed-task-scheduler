package com.dts.scheduler.config;

import com.dts.scheduler.entity.*;
import com.dts.scheduler.repository.TaskRepository;
import com.dts.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("Seeding initial demo users and sample scheduled tasks...");

        // Admin User
        User admin = User.builder()
                .name("System Administrator")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(Role.ROLE_ADMIN)
                .build();
        admin = userRepository.save(admin);

        // Standard Demo User
        User demoUser = User.builder()
                .name("Demo Engineer")
                .email("demo@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.ROLE_USER)
                .build();
        demoUser = userRepository.save(demoUser);

        Instant now = Instant.now();

        // 1. Every-minute webhook task
        Task webhookTask = Task.builder()
                .user(demoUser)
                .name("Every-Minute Status Webhook")
                .description("Dispatches periodic heartbeat ping to remote webhook receiver")
                .taskType(TaskType.WEBHOOK_TASK)
                .payload("{\"url\":\"https://httpbin.org/post\",\"method\":\"POST\"}")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSeconds(60L)
                .nextRunAt(now.plusSeconds(30))
                .status(TaskStatus.ACTIVE)
                .concurrencyPolicy(ConcurrencyPolicy.ALLOW_CONCURRENT)
                .retryPolicy(RetryPolicy.EXPONENTIAL_BACKOFF)
                .maxRetries(3)
                .retryDelaySeconds(5)
                .backoffMultiplier(2.0)
                .maxRetryDelaySeconds(60)
                .timeoutSeconds(15)
                .enabled(true)
                .build();
        taskRepository.save(webhookTask);

        // 2. Hourly API health task (CRON)
        Task hourlyApiTask = Task.builder()
                .user(demoUser)
                .name("Hourly API Health Probe")
                .description("Probes external monitoring gateway using 6-part cron")
                .taskType(TaskType.HTTP_TASK)
                .payload("{\"url\":\"https://httpbin.org/get\",\"method\":\"GET\"}")
                .scheduleType(ScheduleType.CRON)
                .cronExpression("0 0 * * * *")
                .timezone("UTC")
                .nextRunAt(now.plusSeconds(120))
                .status(TaskStatus.ACTIVE)
                .concurrencyPolicy(ConcurrencyPolicy.FORBID_CONCURRENT)
                .retryPolicy(RetryPolicy.EXPONENTIAL_BACKOFF)
                .maxRetries(3)
                .retryDelaySeconds(10)
                .backoffMultiplier(2.0)
                .maxRetryDelaySeconds(300)
                .timeoutSeconds(20)
                .enabled(true)
                .build();
        taskRepository.save(hourlyApiTask);

        // 3. Daily internal maintenance task
        Task dailyMaintenanceTask = Task.builder()
                .user(admin)
                .name("Daily JVM Health Audit")
                .description("Internal memory inspection and processor capability check")
                .taskType(TaskType.INTERNAL_TASK)
                .payload("SYSTEM_HEALTH_CHECK")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSeconds(3600L)
                .nextRunAt(now.plusSeconds(60))
                .status(TaskStatus.ACTIVE)
                .concurrencyPolicy(ConcurrencyPolicy.ALLOW_CONCURRENT)
                .retryPolicy(RetryPolicy.FIXED_DELAY)
                .maxRetries(2)
                .retryDelaySeconds(10)
                .backoffMultiplier(1.0)
                .maxRetryDelaySeconds(30)
                .timeoutSeconds(30)
                .enabled(true)
                .build();
        taskRepository.save(dailyMaintenanceTask);

        // 4. Failing task for demonstrating Exponential Backoff, Retries, and DLQ
        Task failingRetryDemoTask = Task.builder()
                .user(demoUser)
                .name("Failing Task (Retry & DLQ Demo)")
                .description("Simulates intentional execution failures to verify retry backoff and Dead-Letter Queue transitions")
                .taskType(TaskType.INTERNAL_TASK)
                .payload("MOCK_FAILURE_JOB")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSeconds(300L)
                .nextRunAt(now.plusSeconds(300))
                .status(TaskStatus.ACTIVE)
                .concurrencyPolicy(ConcurrencyPolicy.FORBID_CONCURRENT)
                .retryPolicy(RetryPolicy.EXPONENTIAL_BACKOFF)
                .maxRetries(3)
                .retryDelaySeconds(2)
                .backoffMultiplier(2.0)
                .maxRetryDelaySeconds(30)
                .timeoutSeconds(10)
                .enabled(true)
                .build();
        taskRepository.save(failingRetryDemoTask);

        log.info("Demo seeding completed successfully! Credentials: demo@example.com / password123, admin@example.com / admin123");
    }
}
