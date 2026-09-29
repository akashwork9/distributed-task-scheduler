import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { StatusBadge } from '../components/StatusBadge';
import { Link } from 'react-router-dom';
import {
  CalendarClock,
  PlayCircle,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Cpu,
  ArrowUpRight,
  TrendingUp,
  Activity,
  Layers,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
  PieChart,
  Pie,
} from 'recharts';

export const DashboardPage: React.FC = () => {
  const { data: metrics, isLoading: metricsLoading } = useQuery({
    queryKey: ['metrics'],
    queryFn: api.getMetrics,
    refetchInterval: 5000,
  });

  const { data: recentExecutions } = useQuery({
    queryKey: ['recent-executions'],
    queryFn: () => api.getAllExecutions(0, 7),
    refetchInterval: 5000,
  });

  const stats = [
    {
      label: 'Active Tasks',
      value: metrics?.activeTasks ?? 0,
      total: metrics?.totalTasks ?? 0,
      sub: `${metrics?.totalTasks ?? 0} total registered`,
      icon: CalendarClock,
      color: 'text-emerald-400',
      bg: 'bg-emerald-500/10 border-emerald-500/20',
    },
    {
      label: 'Running Executions',
      value: metrics?.runningExecutions ?? 0,
      sub: 'Currently on worker fleet',
      icon: PlayCircle,
      color: 'text-blue-400',
      bg: 'bg-blue-500/10 border-blue-500/20',
    },
    {
      label: 'Success Rate',
      value: `${metrics?.successRatePercentage ?? 100}%`,
      sub: `${metrics?.successExecutions ?? 0} successful runs`,
      icon: CheckCircle2,
      color: 'text-teal-400',
      bg: 'bg-teal-500/10 border-teal-500/20',
    },
    {
      label: 'Failures & DLQ',
      value: metrics?.failedExecutions ?? 0,
      sub: `${metrics?.retryingExecutions ?? 0} currently retrying`,
      icon: AlertTriangle,
      color: 'text-rose-400',
      bg: 'bg-rose-500/10 border-rose-500/20',
    },
    {
      label: 'Active Workers',
      value: metrics?.activeWorkers ?? 0,
      sub: `${metrics?.offlineWorkers ?? 0} offline nodes`,
      icon: Cpu,
      color: 'text-purple-400',
      bg: 'bg-purple-500/10 border-purple-500/20',
    },
  ];

  const executionBreakdownData = [
    { name: 'Success', count: metrics?.successExecutions ?? 0, color: '#10b981' },
    { name: 'Running', count: metrics?.runningExecutions ?? 0, color: '#3b82f6' },
    { name: 'Retrying', count: metrics?.retryingExecutions ?? 0, color: '#f59e0b' },
    { name: 'Failed', count: metrics?.failedExecutions ?? 0, color: '#ef4444' },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <Activity className="w-6 h-6 text-emerald-400" />
            Cluster Control Plane
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time telemetry, leaderless distributed scheduling, and worker state
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to="/tasks/create"
            className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm font-semibold transition-colors shadow-sm"
          >
            Create Scheduled Task
            <ArrowUpRight className="w-4 h-4" />
          </Link>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {stats.map((stat, idx) => {
          const Icon = stat.icon;
          return (
            <div
              key={idx}
              className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 hover:border-slate-700 transition-colors shadow-sm"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                  {stat.label}
                </span>
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center border ${stat.bg} ${stat.color}`}>
                  <Icon className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3">
                <span className="text-2xl font-bold text-white tracking-tight font-mono">
                  {metricsLoading ? '—' : stat.value}
                </span>
                <p className="text-xs text-slate-500 mt-1">{stat.sub}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Execution Breakdown Bar Chart */}
        <div className="lg:col-span-2 bg-slate-900/90 border border-slate-800 rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-base font-semibold text-white">Execution Status Distribution</h3>
              <p className="text-xs text-slate-400">Total job transitions recorded in state store</p>
            </div>
            <TrendingUp className="w-4 h-4 text-slate-400" />
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={executionBreakdownData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <XAxis dataKey="name" stroke="#64748b" fontSize={12} tickLine={false} />
                <YAxis stroke="#64748b" fontSize={12} tickLine={false} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#1e293b', borderRadius: '8px' }}
                  itemStyle={{ color: '#f8fafc' }}
                />
                <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                  {executionBreakdownData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Distributed Architecture Status Card */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-base font-semibold text-white">Distributed Core</h3>
              <Layers className="w-4 h-4 text-emerald-400" />
            </div>
            <p className="text-xs text-slate-400 mb-6">
              Active distributed consensus, locking, and partitioned messaging protocols
            </p>

            <div className="space-y-4">
              <div className="p-3 bg-slate-950/70 border border-slate-800 rounded-lg flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold text-slate-200">Redis Distributed Lock</p>
                  <p className="text-[11px] text-slate-400">Redisson / SETNX with lease TTL</p>
                </div>
                <span className="text-xs font-mono font-semibold text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800">
                  ONLINE
                </span>
              </div>

              <div className="p-3 bg-slate-950/70 border border-slate-800 rounded-lg flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold text-slate-200">Apache Kafka Cluster</p>
                  <p className="text-[11px] text-slate-400">5 Event Topics • 3 Partitions</p>
                </div>
                <span className="text-xs font-mono font-semibold text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800">
                  STREAMING
                </span>
              </div>

              <div className="p-3 bg-slate-950/70 border border-slate-800 rounded-lg flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold text-slate-200">PostgreSQL ACID Store</p>
                  <p className="text-[11px] text-slate-400">Optimistic Locks & Unique Idempotency</p>
                </div>
                <span className="text-xs font-mono font-semibold text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800">
                  HEALTHY
                </span>
              </div>
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-slate-800">
            <Link
              to="/workers"
              className="text-xs text-emerald-400 hover:text-emerald-300 font-medium flex items-center justify-between"
            >
              <span>Inspect active worker consumer group</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      </div>

      {/* Recent Executions Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
          <div>
            <h3 className="text-base font-semibold text-white">Live Execution Feed</h3>
            <p className="text-xs text-slate-400">Real-time task executions consumed by worker pool</p>
          </div>
          <Link
            to="/executions"
            className="text-xs text-emerald-400 hover:text-emerald-300 font-medium flex items-center gap-1"
          >
            <span>View All</span>
            <ArrowUpRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="px-6 py-3">Execution ID</th>
                <th className="px-6 py-3">Task Name</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3">Attempt</th>
                <th className="px-6 py-3">Worker Node</th>
                <th className="px-6 py-3">Latency</th>
                <th className="px-6 py-3">Scheduled</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {recentExecutions?.content.map((exec) => (
                <tr key={exec.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="px-6 py-3 font-mono text-xs text-slate-400">
                    <Link
                      to={`/executions/${exec.id}`}
                      className="text-emerald-400 hover:underline hover:text-emerald-300"
                    >
                      {exec.executionId.substring(0, 13)}...
                    </Link>
                  </td>
                  <td className="px-6 py-3 font-medium text-slate-200">
                    <Link to={`/tasks/${exec.taskId}`} className="hover:underline">
                      {exec.taskName}
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
                    {new Date(exec.scheduledAt).toLocaleTimeString()}
                  </td>
                </tr>
              ))}
              {(!recentExecutions || recentExecutions.content.length === 0) && (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-slate-500">
                    No executions recorded yet. Create a task or trigger one to see live execution data.
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
