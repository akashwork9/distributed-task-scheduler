import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { StatusBadge } from '../components/StatusBadge';
import { ExecutionTimeline } from '../components/ExecutionTimeline';
import {
  PlaySquare,
  ArrowLeft,
  RefreshCw,
  Clock,
  Cpu,
  AlertTriangle,
  CheckCircle2,
  FileCode,
  Layers,
} from 'lucide-react';

export const ExecutionDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const executionId = Number(id);

  const queryClient = useQueryClient();

  const { data: execution, isLoading } = useQuery({
    queryKey: ['execution', executionId],
    queryFn: () => api.getExecutionById(executionId),
    refetchInterval: 3000,
  });

  const retryMutation = useMutation({
    mutationFn: () => api.retryExecution(executionId),
    onSuccess: (newExec) => {
      queryClient.invalidateQueries({ queryKey: ['execution', executionId] });
      queryClient.invalidateQueries({ queryKey: ['executions'] });
      window.location.href = `/executions/${newExec.id}`;
    },
  });

  if (isLoading) {
    return <div className="text-slate-400 p-8">Loading execution details...</div>;
  }

  if (!execution) {
    return <div className="text-slate-400 p-8">Execution record not found</div>;
  }

  return (
    <div className="space-y-8 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to="/executions"
            className="p-2 text-slate-400 hover:text-white bg-slate-900 border border-slate-800 rounded-lg hover:border-slate-700 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold tracking-tight text-white font-mono">
                {execution.executionId.substring(0, 18)}...
              </h1>
              <StatusBadge status={execution.status} />
            </div>
            <p className="text-sm text-slate-400 mt-1">
              Task:{' '}
              <Link
                to={`/tasks/${execution.taskId}`}
                className="text-emerald-400 hover:underline font-semibold"
              >
                {execution.taskName}
              </Link>{' '}
              (ID #{execution.taskId})
            </p>
          </div>
        </div>

        {(execution.status === 'FAILED' || execution.status === 'CANCELLED') && (
          <button
            onClick={() => retryMutation.mutate()}
            disabled={retryMutation.isPending}
            className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm font-semibold transition-colors shadow-sm disabled:opacity-50"
          >
            <RefreshCw className="w-4 h-4" />
            {retryMutation.isPending ? 'Queuing Retry...' : 'Retry Execution'}
          </button>
        )}
      </div>

      {/* Lifecycle Timeline Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 shadow-sm">
        <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-2">
          Execution Lifecycle Flow
        </h3>
        <ExecutionTimeline
          status={execution.status}
          attempt={execution.attempt}
          scheduledAt={execution.scheduledAt}
          startedAt={execution.startedAt}
          completedAt={execution.completedAt}
        />
      </div>

      {/* Execution Diagnostics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-3">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
            Execution Attributes & Worker Assignment
          </span>
          <div className="space-y-2.5 text-xs">
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Execution UUID</span>
              <span className="font-mono text-slate-200">{execution.executionId}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Assigned Worker</span>
              <span className="font-mono text-slate-200 font-semibold">
                {execution.workerId ? execution.workerId : 'Pending Claim by Worker Pool'}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Attempt Count</span>
              <span className="font-mono text-slate-200">Attempt #{execution.attempt}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Execution Latency</span>
              <span className="font-mono text-emerald-400 font-semibold">
                {execution.durationMs ? `${execution.durationMs}ms` : '—'}
              </span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-slate-400">Current Status</span>
              <span className="font-mono text-slate-200 font-semibold">{execution.status}</span>
            </div>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-3">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
            Execution Timestamps (UTC)
          </span>
          <div className="space-y-2.5 text-xs">
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Scheduled At</span>
              <span className="font-mono text-slate-200">
                {new Date(execution.scheduledAt).toLocaleString()}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Started At</span>
              <span className="font-mono text-slate-200">
                {execution.startedAt ? new Date(execution.startedAt).toLocaleString() : '—'}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/80">
              <span className="text-slate-400">Completed At</span>
              <span className="font-mono text-slate-200">
                {execution.completedAt ? new Date(execution.completedAt).toLocaleString() : '—'}
              </span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-slate-400">Database Record Created</span>
              <span className="font-mono text-slate-200">
                {new Date(execution.createdAt).toLocaleString()}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Error Message Card (If any) */}
      {execution.errorMessage && (
        <div className="bg-rose-950/40 border border-rose-800/80 rounded-xl p-5 space-y-2">
          <div className="flex items-center gap-2 text-rose-400 font-semibold text-sm">
            <AlertTriangle className="w-4 h-4" />
            <span>Execution Failure Diagnostics & Error Trace</span>
          </div>
          <pre className="p-3 bg-slate-950/90 rounded-lg text-xs font-mono text-rose-300 overflow-x-auto border border-rose-900/50">
            {execution.errorMessage}
          </pre>
        </div>
      )}

      {/* Result Output Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-3">
        <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
          Execution Result Output
        </span>
        <pre className="p-4 bg-slate-950 rounded-lg text-xs font-mono text-slate-200 overflow-x-auto border border-slate-800 max-h-96">
          {execution.result ? execution.result : 'No output data recorded or execution is currently running.'}
        </pre>
      </div>
    </div>
  );
};
