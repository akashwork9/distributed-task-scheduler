package com.dts.scheduler.worker;

import com.dts.scheduler.entity.TaskType;
import com.dts.scheduler.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskExecutorFactory {

    private final List<TaskExecutor> executors;

    public TaskExecutor getExecutor(TaskType taskType) {
        return executors.stream()
                .filter(executor -> executor.supports(taskType))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No executor registered for task type: " + taskType));
    }
}
