package com.dts.scheduler.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerResponse {
    private Long id;
    private Long taskId;
    private String executionId;
    private String status;
    private Instant triggeredAt;
    private String message;
}
