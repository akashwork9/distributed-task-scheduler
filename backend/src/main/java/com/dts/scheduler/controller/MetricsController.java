package com.dts.scheduler.controller;

import com.dts.scheduler.dto.metrics.SystemMetricsResponse;
import com.dts.scheduler.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Endpoints for real-time task scheduling and execution metrics")
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping
    @Operation(summary = "Get system metrics", description = "Retrieves aggregated counts of tasks, executions, success rates, and worker health")
    public ResponseEntity<SystemMetricsResponse> getMetrics() {
        return ResponseEntity.ok(metricsService.getSystemMetrics());
    }
}
