import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { StatusBadge } from '../components/StatusBadge';
import {
  CalendarClock,
  ArrowLeft,
  Play,
  Pause,
  RotateCcw,
  XCircle,
  ExternalLink,
  Clock,
  Shield,
  Layers,
  CheckCircle,
} from 'lucide-react';

export const TaskDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const taskId = Number(id);
  const [page, setPage] = useState(0);

  const queryClient = useQueryClient();

  const { data: task, isLoading: taskLoading } = useQuery({
    queryKey: ['task', taskId],
    queryFn: () => api.getTaskById(taskId),
    refetchInterval: 5000,
  });

  const { data: executions, isLoading: execLoading } = useQuery({
    queryKey: ['task-executions', taskId, page],
    queryFn: () => api.getExecutionsForTask(taskId, page, 10),
    refetchInterval: 5000,
  });

  const triggerMutation = useMutation({
    mutationFn: () => api.triggerTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['task', taskId] });
      queryClient.invalidateQueries({ queryKey: ['task-executions', taskId] });
      queryClient.invalidateQueries({ queryKey: ['metrics'] });
    },
  });

  const pauseMutation = useMutation({
    mutationFn: () => api.pauseTask(taskId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['task', taskId] }),
  });

  const resumeMutation = useMutation({
    mutationFn: () => api.resumeTask(taskId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['task', taskId] }),
  });

  const cancelMutation = useMutation({
    mutationFn: () => api.cancelTask(taskId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['task', taskId] }),
  });

  if (taskLoading) {
    return <div className="text-slate-400 p-8">Loading task details...</div>;
  }

  if (!task) {
    return <div className="text-slate-400 p-8">Task not found</div>;
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to="/tasks"
            className="p-2 text-slate-400 hover:text-white bg-slate-900 border border-slate-800 rounded-lg hover:border-slate-700 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold tracking-tight text-white">{task.name}</h1>
              <StatusBadge status={task.status} />
            </div>
            {task.description && (
              <p className="text-sm text-slate-400 mt-1">{task.description}</p>
            )}
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => triggerMutation.mutate()}
            disabled={triggerMutation.isPending}
            className="inline-flex items-center gap-2 px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm font-semibold transition-colors shadow-sm disabled:opacity-50"
          >
            <Play className="w-4 h-4" />
            Trigger Now
          </button>

          {task.status === 'ACTIVE' ? (
            <button
              onClick={() => pauseMutation.mutate()}
              className="inline-flex items-center gap-2 px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-amber-400 border border-slate-800 rounded-lg text-sm font-semibold transition-colors"
            >
              <Pause className="w-4 h-4" />
              Pause
            </button>
          ) : task.status === 'PAUSED' ? (
            <button
              onClick={() => resumeMutation.mutate()}
              className="inline-flex items-center gap-2 px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-emerald-400 border border-slate-800 rounded-lg text-sm font-semibold transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              Resume
            </button>
          ) : null}

          {task.status !== 'CANCELLED' && (
            <button
              onClick={() => cancelMutation.mutate()}
              className="inline-flex items-center gap-2 px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-slate-400 border border-slate-800 rounded-lg text-sm font-semibold transition-colors"
            >
              <XCircle className="w-4 h-4" />
              Cancel
            </button>
          )}
        </div>
      </div>

      {/* Grid: Task Specification */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-3">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
            Schedule Specification
          </span>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Schedule Type</span>
              <span className="font-mono text-slate-200 font-semibold">{task.scheduleType}</span>
            </div>
            {task.cronExpression && (
              <div className="flex justify-between py-1 border-b border-slate-800/80">
                <span className="text-slate-400">Cron Expression</span>
                <span className="font-mono text-emerald-400 font-semibold">{task.cronExpression}</span>
              </div>
            )}
            {task.intervalSeconds && (
              <div className="flex justify-between py-1 border-b border-slate-800/80">
                <span className="text-slate-400">Interval</span>
                <span className="font-mono text-slate-200">Every {task.intervalSeconds}s</span>
              </div>
            )}
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Timezone</span>
              <span className="font-mono text-slate-200">{task.timezone}</span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-slate-400">Next Scheduled At</span>
              <span className="font-mono text-slate-200">
                {task.nextRunAt ? new Date(task.nextRunAt).toLocaleString() : '—'}
              </span>
            </div>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-3">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
            Resilience & Execution Policy
          </span>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Concurrency Control</span>
              <span className="font-mono text-slate-200">{task.concurrencyPolicy}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Max Retries</span>
              <span className="font-mono text-slate-200">{task.maxRetries}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Initial Backoff Delay</span>
              <span className="font-mono text-slate-200">{task.retryDelaySeconds}s</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Backoff Multiplier</span>
              <span className="font-mono text-slate-200">{task.backoffMultiplier}x</span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-slate-400">Execution Timeout</span>
              <span className="font-mono text-slate-200">{task.timeoutSeconds}s</span>
            </div>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-3">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
            Task Metadata & Versioning
          </span>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Task ID</span>
              <span className="font-mono text-slate-200">#{task.id}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Owner User</span>
              <span className="text-slate-200">{task.userName}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Optimistic Lock Version</span>
              <span className="font-mono text-slate-200">v{task.version}</span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-slate-400">Created At</span>
              <span className="text-slate-200">{new Date(task.createdAt).toLocaleString()}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Payload Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-3">
        <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
          Task Payload ({task.taskType})
        </span>
        <pre className="p-4 bg-slate-950 rounded-lg text-xs font-mono text-emerald-400 overflow-x-auto border border-slate-800">
          {task.payload}
        </pre>
      </div>

      {/* Execution History */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
          <h3 className="text-base font-semibold text-white">Executions for This Task</h3>
          <span className="text-xs font-mono text-slate-400">
            Total Runs: {executions?.totalElements ?? 0}
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="px-6 py-3">Execution ID</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3">Attempt</th>
                <th className="px-6 py-3">Worker Node</th>
                <th className="px-6 py-3">Latency</th>
                <th className="px-6 py-3">Scheduled</th>
                <th className="px-6 py-3 text-right">Inspect</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {executions?.content.map((exec) => (
                <tr key={exec.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="px-6 py-3 font-mono text-xs text-slate-400">
                    <Link
                      to={`/executions/${exec.id}`}
                      className="text-emerald-400 hover:underline hover:text-emerald-300"
                    >
                      {exec.executionId.substring(0, 13)}...
                    </Link>
                  </td>
                  <td className="px-6 py-3">
                    <StatusBadge status={exec.status} size="sm" />
                  </td>
                  <td className="px-6 py-3 font-mono text-xs">#{exec.attempt}</td>
                  <td className="px-6 py-3 font-mono text-xs text-slate-400">
                    {exec.workerId || '—'}
                  </td>
                  <td className="px-6 py-3 font-mono text-xs">
                    {exec.durationMs ? `${exec.durationMs}ms` : '—'}
                  </td>
                  <td className="px-6 py-3 text-xs text-slate-400">
                    {new Date(exec.scheduledAt).toLocaleString()}
                  </td>
                  <td className="px-6 py-3 text-right">
                    <Link
                      to={`/executions/${exec.id}`}
                      className="text-xs text-emerald-400 hover:text-emerald-300 font-medium inline-flex items-center gap-1"
                    >
                      <span>Details</span>
                      <ExternalLink className="w-3 h-3" />
                    </Link>
                  </td>
                </tr>
              ))}
              {(!executions || executions.content.length === 0) && !execLoading && (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-slate-500">
                    No executions triggered yet for this task. Click "Trigger Now" above to test immediate execution.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
