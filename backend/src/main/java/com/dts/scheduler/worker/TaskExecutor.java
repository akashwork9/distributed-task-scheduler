package com.dts.scheduler.worker;

import com.dts.scheduler.entity.TaskType;
import com.dts.scheduler.kafka.TaskExecutionEvent;

public interface TaskExecutor {
    boolean supports(TaskType taskType);
    TaskExecutionResult execute(TaskExecutionEvent event);
}
