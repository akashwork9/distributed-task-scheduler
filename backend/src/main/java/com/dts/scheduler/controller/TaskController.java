package com.dts.scheduler.controller;

import com.dts.scheduler.dto.PageResponse;
import com.dts.scheduler.dto.task.CreateTaskRequest;
import com.dts.scheduler.dto.task.TaskResponse;
import com.dts.scheduler.dto.task.UpdateTaskRequest;
import com.dts.scheduler.entity.TaskStatus;
import com.dts.scheduler.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Endpoints for managing scheduled background tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create scheduled task", description = "Defines a new scheduled task (ONE_TIME, INTERVAL, or CRON)")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List tasks", description = "Returns a paginated list of tasks for the authenticated user")
    public ResponseEntity<PageResponse<TaskResponse>> getTasks(
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasks(pageable, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task details", description = "Retrieves full configuration and status for a specific task")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Updates an existing task configuration with optimistic lock safety")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task", description = "Permanently deletes a task and associated execution records")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause task", description = "Suspends subsequent scheduled executions for this task")
    public ResponseEntity<TaskResponse> pauseTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.pauseTask(id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume task", description = "Re-activates a paused task and calculates the next execution timestamp")
    public ResponseEntity<TaskResponse> resumeTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.resumeTask(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel task", description = "Cancels a task permanently")
    public ResponseEntity<TaskResponse> cancelTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.cancelTask(id));
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "Trigger task execution immediately", description = "Bypasses next schedule and queues immediate execution")
    public ResponseEntity<com.dts.scheduler.dto.task.TriggerResponse> triggerTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.triggerTaskImmediately(id));
    }
}
