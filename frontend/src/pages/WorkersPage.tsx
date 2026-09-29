import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { StatusBadge } from '../components/StatusBadge';
import { Cpu, Radio, Server, CheckCircle2, Clock, Activity } from 'lucide-react';

export const WorkersPage: React.FC = () => {
  const { data: workers, isLoading } = useQuery({
    queryKey: ['workers'],
    queryFn: api.getAllWorkers,
    refetchInterval: 4000,
  });

  const totalCapacity = workers?.reduce((sum, w) => sum + w.capacity, 0) ?? 0;
  const activeJobs = workers?.reduce((sum, w) => sum + w.activeJobs, 0) ?? 0;
  const activeNodes = workers?.filter((w) => w.status === 'ACTIVE').length ?? 0;
  const utilizationPercent = totalCapacity > 0 ? Math.round((activeJobs / totalCapacity) * 100) : 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <Cpu className="w-6 h-6 text-purple-400" />
            Worker Cluster & Consumer Fleet
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time worker registration, heartbeat telemetry, and concurrency capacity
          </p>
        </div>
      </div>

      {/* Cluster Capacity Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
            Active Worker Replicas
          </span>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-3xl font-bold text-white font-mono">{activeNodes}</span>
            <span className="text-xs text-slate-500 font-mono">/ {workers?.length ?? 0} total</span>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
            Fleet Concurrency Slots
          </span>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-3xl font-bold text-white font-mono">{totalCapacity}</span>
            <span className="text-xs text-slate-500 font-mono">total job slots</span>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
            Cluster Utilization
          </span>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-3xl font-bold text-emerald-400 font-mono">{utilizationPercent}%</span>
            <span className="text-xs text-slate-500 font-mono">({activeJobs} jobs active)</span>
          </div>
          <div className="w-full bg-slate-950 rounded-full h-1.5 mt-3 overflow-hidden">
            <div
              className="bg-emerald-500 h-1.5 rounded-full transition-all"
              style={{ width: `${Math.min(100, Math.max(5, utilizationPercent))}%` }}
            />
          </div>
        </div>
      </div>

      {/* Workers Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
          <h3 className="text-base font-semibold text-white">Registered Worker Nodes</h3>
          <span className="text-xs font-mono text-emerald-400 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            Heartbeat Interval: 10s
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="px-6 py-3.5">Worker ID</th>
                <th className="px-6 py-3.5">Hostname / IP</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Active Jobs</th>
                <th className="px-6 py-3.5">Slot Capacity</th>
                <th className="px-6 py-3.5">Last Heartbeat</th>
                <th className="px-6 py-3.5 text-right">Age</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {workers?.map((w) => {
                const lastHeartbeatDate = new Date(w.lastHeartbeat);
                const secondsAgo = Math.max(0, Math.round((Date.now() - lastHeartbeatDate.getTime()) / 1000));
                return (
                  <tr key={w.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="px-6 py-4 font-mono text-xs font-semibold text-white flex items-center gap-2">
                      <Server className="w-4 h-4 text-purple-400" />
                      <span>{w.workerId}</span>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-400">
                      <div>{w.hostname}</div>
                      <div className="text-[11px] text-slate-500">{w.ipAddress || '127.0.0.1'}</div>
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={w.status} size="sm" />
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-200">
                      {w.activeJobs} running
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-200">
                      {w.capacity} concurrent
                    </td>
                    <td className="px-6 py-4 text-xs font-mono text-slate-400">
                      {lastHeartbeatDate.toLocaleTimeString()}
                    </td>
                    <td className="px-6 py-4 text-right font-mono text-xs">
                      <span className={secondsAgo > 30 ? 'text-rose-400 font-bold' : 'text-slate-400'}>
                        {secondsAgo}s ago
                      </span>
                    </td>
                  </tr>
                );
              })}

              {(!workers || workers.length === 0) && !isLoading && (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-slate-500">
                    No active worker instances registered. Ensure at least one worker service instance is running.
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
