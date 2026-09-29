import axios from 'axios';
import {
  AuthResponse,
  CreateTaskRequest,
  PageResponse,
  SystemMetrics,
  Task,
  TaskExecution,
  UpdateTaskRequest,
  User,
  Worker,
  AuditLog,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT Bearer token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('dts_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for 401 handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('dts_token');
      localStorage.removeItem('dts_user');
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export const api = {
  // Auth
  login: async (credentials: { email: string; password: string }): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/login', credentials);
    return res.data;
  },
  register: async (data: { name: string; email: string; password: string }): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/register', data);
    return res.data;
  },
  getCurrentUser: async (): Promise<User> => {
    const res = await apiClient.get<User>('/auth/me');
    return res.data;
  },

  // Tasks
  getTasks: async (page = 0, size = 20, status?: string): Promise<PageResponse<Task>> => {
    const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
    if (status) params.append('status', status);
    const res = await apiClient.get<PageResponse<Task>>(`/tasks?${params.toString()}`);
    return res.data;
  },
  getTaskById: async (id: number): Promise<Task> => {
    const res = await apiClient.get<Task>(`/tasks/${id}`);
    return res.data;
  },
  createTask: async (data: CreateTaskRequest): Promise<Task> => {
    const res = await apiClient.post<Task>('/tasks', data);
    return res.data;
  },
  updateTask: async (id: number, data: UpdateTaskRequest): Promise<Task> => {
    const res = await apiClient.put<Task>(`/tasks/${id}`, data);
    return res.data;
  },
  deleteTask: async (id: number): Promise<void> => {
    await apiClient.delete(`/tasks/${id}`);
  },
  pauseTask: async (id: number): Promise<Task> => {
    const res = await apiClient.post<Task>(`/tasks/${id}/pause`);
    return res.data;
  },
  resumeTask: async (id: number): Promise<Task> => {
    const res = await apiClient.post<Task>(`/tasks/${id}/resume`);
    return res.data;
  },
  cancelTask: async (id: number): Promise<Task> => {
    const res = await apiClient.post<Task>(`/tasks/${id}/cancel`);
    return res.data;
  },
  triggerTask: async (id: number): Promise<{ executionId: string; message: string }> => {
    const res = await apiClient.post<{ executionId: string; message: string }>(`/tasks/${id}/trigger`);
    return res.data;
  },

  // Executions
  getExecutionsForTask: async (taskId: number, page = 0, size = 20): Promise<PageResponse<TaskExecution>> => {
    const res = await apiClient.get<PageResponse<TaskExecution>>(`/tasks/${taskId}/executions?page=${page}&size=${size}`);
    return res.data;
  },
  getAllExecutions: async (page = 0, size = 20): Promise<PageResponse<TaskExecution>> => {
    const res = await apiClient.get<PageResponse<TaskExecution>>(`/executions?page=${page}&size=${size}`);
    return res.data;
  },
  getExecutionById: async (id: number): Promise<TaskExecution> => {
    const res = await apiClient.get<TaskExecution>(`/executions/${id}`);
    return res.data;
  },
  retryExecution: async (id: number): Promise<TaskExecution> => {
    const res = await apiClient.post<TaskExecution>(`/executions/${id}/retry`);
    return res.data;
  },

  // Workers
  getAllWorkers: async (): Promise<Worker[]> => {
    const res = await apiClient.get<Worker[]>('/workers');
    return res.data;
  },

  // Metrics
  getMetrics: async (): Promise<SystemMetrics> => {
    const res = await apiClient.get<SystemMetrics>('/metrics');
    return res.data;
  },

  // Audit Logs
  getAuditLogs: async (page = 0, size = 20): Promise<PageResponse<AuditLog>> => {
    const res = await apiClient.get<PageResponse<AuditLog>>(`/audit-logs?page=${page}&size=${size}`);
    return res.data;
  },
};
