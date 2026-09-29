package com.dts.scheduler.dto.worker;

import com.dts.scheduler.entity.WorkerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerResponse {
    private Long id;
    private String workerId;
    private String hostname;
    private String ipAddress;
    private WorkerStatus status;
    private Instant lastHeartbeat;
    private int activeJobs;
    private int capacity;
    private Instant registeredAt;
}
