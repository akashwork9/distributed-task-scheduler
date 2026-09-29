package com.dts.scheduler.controller;

import com.dts.scheduler.dto.PageResponse;
import com.dts.scheduler.dto.execution.TaskExecutionResponse;
import com.dts.scheduler.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Executions", description = "Endpoints for inspecting execution logs and retrying failed tasks")
public class ExecutionController {

    private final ExecutionService executionService;

    @GetMapping("/api/tasks/{taskId}/executions")
    @Operation(summary = "Get task execution history", description = "Retrieves execution history for a given task")
    public ResponseEntity<PageResponse<TaskExecutionResponse>> getExecutionsForTask(
            @PathVariable Long taskId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(executionService.getExecutionsByTaskId(taskId, pageable));
    }

    @GetMapping("/api/executions/{id}")
    @Operation(summary = "Get execution details", description = "Retrieves deep details, status, worker assignment, and output for an execution by numeric ID or execution UUID")
    public ResponseEntity<TaskExecutionResponse> getExecutionById(@PathVariable String id) {
        return ResponseEntity.ok(executionService.getExecutionById(id));
    }

    @GetMapping("/api/executions")
    @Operation(summary = "List all executions", description = "Returns system-wide or user-scoped execution history")
    public ResponseEntity<PageResponse<TaskExecutionResponse>> getAllExecutions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(executionService.getAllExecutions(pageable));
    }

    @PostMapping("/api/executions/{id}/retry")
    @Operation(summary = "Retry execution", description = "Dispatches a new execution retry for a failed or dead-letter task")
    public ResponseEntity<TaskExecutionResponse> retryExecution(@PathVariable Long id) {
        return ResponseEntity.ok(executionService.retryExecution(id));
    }
}
