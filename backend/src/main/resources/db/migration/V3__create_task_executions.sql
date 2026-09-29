-- V3: Create Task Executions Table
CREATE TABLE IF NOT EXISTS task_executions (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    attempt INT NOT NULL DEFAULT 1,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    worker_id VARCHAR(100),
    error_message TEXT,
    result TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_executions_execution_id UNIQUE (execution_id),
    CONSTRAINT fk_task_executions_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);
