export type Role = 'ROLE_USER' | 'ROLE_ADMIN';

export type TaskType = 'HTTP_TASK' | 'WEBHOOK_TASK' | 'INTERNAL_TASK';

export type ScheduleType = 'ONE_TIME' | 'INTERVAL' | 'CRON';

export type TaskStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export type ConcurrencyPolicy = 'ALLOW_CONCURRENT' | 'FORBID_CONCURRENT';

export type RetryPolicy = 'EXPONENTIAL_BACKOFF' | 'FIXED_DELAY';

export type ExecutionStatus = 'SCHEDULED' | 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'RETRYING' | 'CANCELLED';

export type WorkerStatus = 'ACTIVE' | 'UNHEALTHY' | 'OFFLINE';

export interface User {
  id: number;
  email: string;
  name: string;
  role: Role;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: User;
}

export interface Task {
  id: number;
  userId: number;
  userName: string;
  name: string;
  description?: string;
  taskType: TaskType;
  payload: string;
  scheduleType: ScheduleType;
  cronExpression?: string;
  timezone: string;
  scheduledAt?: string;
  intervalSeconds?: number;
  nextRunAt?: string;
  lastRunAt?: string;
  status: TaskStatus;
  concurrencyPolicy: ConcurrencyPolicy;
  retryPolicy: RetryPolicy;
  maxRetries: number;
  retryDelaySeconds: number;
  backoffMultiplier: number;
  maxRetryDelaySeconds: number;
  timeoutSeconds: number;
  enabled: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaskExecution {
  id: number;
  taskId: number;
  taskName: string;
  executionId: string;
  status: ExecutionStatus;
  attempt: number;
  scheduledAt: string;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  workerId?: string;
  errorMessage?: string;
  result?: string;
  createdAt: string;
}

export interface Worker {
  id: number;
  workerId: string;
  hostname: string;
  ipAddress?: string;
  status: WorkerStatus;
  lastHeartbeat: string;
  activeJobs: number;
  capacity: number;
  registeredAt: string;
}

export interface AuditLog {
  id: number;
  userId?: number;
  userEmail: string;
  action: string;
  entityType: string;
  entityId: string;
  metadata?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface SystemMetrics {
  totalTasks: number;
  activeTasks: number;
  pausedTasks: number;
  totalExecutions: number;
  runningExecutions: number;
  successExecutions: number;
  failedExecutions: number;
  retryingExecutions: number;
  activeWorkers: number;
  offlineWorkers: number;
  successRatePercentage: number;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

export interface CreateTaskRequest {
  name: string;
  description?: string;
  taskType: TaskType;
  payload: string;
  scheduleType: ScheduleType;
  cronExpression?: string;
  timezone?: string;
  scheduledAt?: string;
  intervalSeconds?: number;
  concurrencyPolicy?: ConcurrencyPolicy;
  retryPolicy?: RetryPolicy;
  maxRetries?: number;
  retryDelaySeconds?: number;
  backoffMultiplier?: number;
  maxRetryDelaySeconds?: number;
  timeoutSeconds?: number;
  enabled?: boolean;
}

export interface UpdateTaskRequest {
  name?: string;
  description?: string;
  taskType?: TaskType;
  payload?: string;
  scheduleType?: ScheduleType;
  cronExpression?: string;
  timezone?: string;
  scheduledAt?: string;
  intervalSeconds?: number;
  concurrencyPolicy?: ConcurrencyPolicy;
  retryPolicy?: RetryPolicy;
  maxRetries?: number;
  retryDelaySeconds?: number;
  backoffMultiplier?: number;
  maxRetryDelaySeconds?: number;
  timeoutSeconds?: number;
  enabled?: boolean;
}
