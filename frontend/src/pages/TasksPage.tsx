import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { Task, TaskStatus } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { Link } from 'react-router-dom';
import {
  CalendarClock,
  Plus,
  Play,
  Pause,
  RotateCcw,
  Trash2,
  XCircle,
  ExternalLink,
  Search,
  Filter,
} from 'lucide-react';

export const TasksPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);

  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['tasks', page, statusFilter],
    queryFn: () => api.getTasks(page, 20, statusFilter || undefined),
    refetchInterval: 5000,
  });

  const triggerMutation = useMutation({
    mutationFn: (id: number) => api.triggerTask(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['recent-executions'] });
      queryClient.invalidateQueries({ queryKey: ['metrics'] });
    },
  });

  const pauseMutation = useMutation({
    mutationFn: (id: number) => api.pauseTask(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] }),
  });

  const resumeMutation = useMutation({
    mutationFn: (id: number) => api.resumeTask(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] }),
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => api.cancelTask(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.deleteTask(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] }),
  });

  const filteredTasks = data?.content.filter((task) =>
    task.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    task.payload.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <CalendarClock className="w-6 h-6 text-emerald-400" />
            Scheduled Tasks
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Configure periodic CRON, interval-based, and one-time execution jobs
          </p>
        </div>
        <Link
          to="/tasks/create"
          className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm font-semibold transition-colors shadow-sm self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          Create Task
        </Link>
      </div>

      {/* Filter / Search Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-center justify-between bg-slate-900/60 p-4 border border-slate-800 rounded-xl">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Search by name or payload..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Filter className="w-4 h-4 text-slate-400 shrink-0" />
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-sm text-slate-300 focus:outline-none focus:ring-1 focus:ring-emerald-500 w-full sm:w-auto"
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="PAUSED">Paused</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>

      {/* Tasks Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="px-6 py-3.5">Task Name</th>
                <th className="px-6 py-3.5">Type</th>
                <th className="px-6 py-3.5">Schedule</th>
                <th className="px-6 py-3.5">Next Execution</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Concurrency</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {filteredTasks?.map((task) => (
                <tr key={task.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="px-6 py-4">
                    <Link
                      to={`/tasks/${task.id}`}
                      className="font-semibold text-white hover:text-emerald-400 flex items-center gap-1.5"
                    >
                      {task.name}
                      <ExternalLink className="w-3.5 h-3.5 opacity-60" />
                    </Link>
                    {task.description && (
                      <p className="text-xs text-slate-400 mt-0.5 line-clamp-1">{task.description}</p>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    <span className="text-xs font-mono font-medium bg-slate-950 px-2.5 py-1 rounded border border-slate-800 text-slate-300">
                      {task.taskType}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-xs font-mono">
                      <span className="text-slate-200 font-semibold">{task.scheduleType}</span>
                      {task.cronExpression && (
                        <p className="text-slate-400 mt-0.5">{task.cronExpression}</p>
                      )}
                      {task.intervalSeconds && (
                        <p className="text-slate-400 mt-0.5">Every {task.intervalSeconds}s</p>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    {task.nextRunAt ? (
                      <span className="text-xs font-mono text-slate-300">
                        {new Date(task.nextRunAt).toLocaleString()}
                      </span>
                    ) : (
                      <span className="text-xs text-slate-500">—</span>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    <StatusBadge status={task.status} size="sm" />
                  </td>
                  <td className="px-6 py-4 text-xs font-mono text-slate-400">
                    {task.concurrencyPolicy === 'ALLOW_CONCURRENT' ? 'ALLOW' : 'FORBID'}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        onClick={() => triggerMutation.mutate(task.id)}
                        disabled={triggerMutation.isPending}
                        title="Trigger Immediately"
                        className="p-1.5 text-slate-400 hover:text-emerald-400 hover:bg-slate-800 rounded-lg transition-colors"
                      >
                        <Play className="w-4 h-4" />
                      </button>

                      {task.status === 'ACTIVE' ? (
                        <button
                          onClick={() => pauseMutation.mutate(task.id)}
                          title="Pause Task"
                          className="p-1.5 text-slate-400 hover:text-amber-400 hover:bg-slate-800 rounded-lg transition-colors"
                        >
                          <Pause className="w-4 h-4" />
                        </button>
                      ) : task.status === 'PAUSED' ? (
                        <button
                          onClick={() => resumeMutation.mutate(task.id)}
                          title="Resume Task"
                          className="p-1.5 text-slate-400 hover:text-emerald-400 hover:bg-slate-800 rounded-lg transition-colors"
                        >
                          <RotateCcw className="w-4 h-4" />
                        </button>
                      ) : null}

                      {task.status !== 'CANCELLED' && (
                        <button
                          onClick={() => cancelMutation.mutate(task.id)}
                          title="Cancel Task"
                          className="p-1.5 text-slate-400 hover:text-zinc-300 hover:bg-slate-800 rounded-lg transition-colors"
                        >
                          <XCircle className="w-4 h-4" />
                        </button>
                      )}

                      <button
                        onClick={() => {
                          if (confirm(`Are you sure you want to delete task "${task.name}"?`)) {
                            deleteMutation.mutate(task.id);
                          }
                        }}
                        title="Delete Task"
                        className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}

              {(!filteredTasks || filteredTasks.length === 0) && !isLoading && (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-slate-500">
                    No scheduled tasks found matching your filter criteria.
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
