package com.dts.scheduler.worker;

import com.dts.scheduler.entity.TaskType;
import com.dts.scheduler.kafka.TaskExecutionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InternalTaskExecutor implements TaskExecutor {

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.INTERNAL_TASK;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionEvent event) {
        long startTime = System.currentTimeMillis();
        String jobIdentifier = event.getPayload() != null ? event.getPayload().trim().toUpperCase() : "DEFAULT";

        log.info("Worker executing INTERNAL_TASK [executionId={}] job={}", event.getExecutionId(), jobIdentifier);

        try {
            return switch (jobIdentifier) {
                case "SYSTEM_HEALTH_CHECK" -> {
                    long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
                    long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
                    String result = String.format("System health OK: Free JVM Memory=%dMB, Total=%dMB, Available Processors=%d",
                            freeMemory, totalMemory, Runtime.getRuntime().availableProcessors());
                    long duration = System.currentTimeMillis() - startTime;
                    yield TaskExecutionResult.success(200, result, duration);
                }
                case "MOCK_HEAVY_CALCULATION" -> {
                    Thread.sleep(500); // Simulate processing work
                    long duration = System.currentTimeMillis() - startTime;
                    yield TaskExecutionResult.success(200, "Computation completed successfully in " + duration + "ms", duration);
                }
                case "MOCK_FAILURE_JOB" -> {
                    long duration = System.currentTimeMillis() - startTime;
                    yield TaskExecutionResult.failure(500, "Simulated intentional failure for retry verification", duration);
                }
                default -> {
                    long duration = System.currentTimeMillis() - startTime;
                    yield TaskExecutionResult.success(200, "Internal job [" + jobIdentifier + "] executed successfully", duration);
                }
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            return TaskExecutionResult.failure(500, "Internal job interrupted", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskExecutionResult.failure(500, "Internal job failed: " + e.getMessage(), duration);
        }
    }
}
