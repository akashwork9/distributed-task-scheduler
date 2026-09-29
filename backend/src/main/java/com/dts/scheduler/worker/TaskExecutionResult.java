package com.dts.scheduler.worker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionResult {
    private boolean success;
    private int statusCode;
    private String resultData;
    private String errorMessage;
    private long durationMs;

    public static TaskExecutionResult success(int statusCode, String resultData, long durationMs) {
        return TaskExecutionResult.builder()
                .success(true)
                .statusCode(statusCode)
                .resultData(resultData)
                .durationMs(durationMs)
                .build();
    }

    public static TaskExecutionResult failure(int statusCode, String errorMessage, long durationMs) {
        return TaskExecutionResult.builder()
                .success(false)
                .statusCode(statusCode)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();
    }
}
