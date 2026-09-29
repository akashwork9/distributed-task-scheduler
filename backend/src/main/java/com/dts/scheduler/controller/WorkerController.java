package com.dts.scheduler.controller;

import com.dts.scheduler.dto.worker.WorkerResponse;
import com.dts.scheduler.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@Tag(name = "Workers", description = "Endpoints for inspecting worker cluster status and node health")
public class WorkerController {

    private final WorkerService workerService;

    @GetMapping
    @Operation(summary = "List registered workers", description = "Returns active and offline worker instances with heartbeat timestamps")
    public ResponseEntity<List<WorkerResponse>> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers());
    }
}
