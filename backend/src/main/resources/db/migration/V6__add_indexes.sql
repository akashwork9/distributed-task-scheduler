-- V6: Add Indexes for High Performance Querying & Distributed Polling
CREATE INDEX IF NOT EXISTS idx_tasks_schedule_poll ON tasks (status, enabled, next_run_at);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks (user_id);
CREATE INDEX IF NOT EXISTS idx_task_executions_task_created ON task_executions (task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_executions_status ON task_executions (status, started_at);
CREATE INDEX IF NOT EXISTS idx_task_executions_execution_id ON task_executions (execution_id);
CREATE INDEX IF NOT EXISTS idx_workers_status_heartbeat ON workers (status, last_heartbeat);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created ON audit_logs (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
