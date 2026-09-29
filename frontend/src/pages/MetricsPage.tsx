import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { BarChart3, ExternalLink, Activity, HardDrive, ShieldCheck, Gauge } from 'lucide-react';

export const MetricsPage: React.FC = () => {
  const { data: metrics } = useQuery({
    queryKey: ['metrics'],
    queryFn: api.getMetrics,
    refetchInterval: 4000,
  });

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <BarChart3 className="w-6 h-6 text-teal-400" />
            System Observability & Telemetry
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Aggregated metrics exported to Prometheus and Actuator telemetry scrapers
          </p>
        </div>

        <div className="flex items-center gap-3">
          <a
            href="/actuator/prometheus"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-900 border border-slate-800 hover:border-slate-700 text-xs font-mono text-slate-300 rounded-lg transition-colors"
          >
            <span>Prometheus Metrics</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
          <a
            href="/swagger-ui/index.html"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-900 border border-slate-800 hover:border-slate-700 text-xs font-mono text-emerald-400 rounded-lg transition-colors"
          >
            <span>OpenAPI Docs</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>

      {/* Grid: Gauges & Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 flex flex-col justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
              Overall Job Execution Success Rate
            </span>
            <p className="text-xs text-slate-500">Completed jobs ending in 2xx / normal return</p>
          </div>
          <div className="my-6 text-center">
            <span className="text-5xl font-extrabold font-mono text-emerald-400 tracking-tight">
              {metrics?.successRatePercentage ?? 100}%
            </span>
          </div>
          <div className="text-xs text-slate-400 border-t border-slate-800 pt-3 flex justify-between">
            <span>Successful Runs:</span>
            <span className="font-mono text-white font-semibold">{metrics?.successExecutions ?? 0}</span>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 flex flex-col justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
              Dead-Letter Queue & Retries
            </span>
            <p className="text-xs text-slate-500">Failures requiring exponential backoff intervention</p>
          </div>
          <div className="my-6 text-center">
            <span className="text-5xl font-extrabold font-mono text-rose-400 tracking-tight">
              {metrics?.failedExecutions ?? 0}
            </span>
          </div>
          <div className="text-xs text-slate-400 border-t border-slate-800 pt-3 flex justify-between">
            <span>Currently Retrying:</span>
            <span className="font-mono text-amber-400 font-semibold">{metrics?.retryingExecutions ?? 0}</span>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 flex flex-col justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
              Active Task Registrations
            </span>
            <p className="text-xs text-slate-500">Live cron & interval schedules in Postgres</p>
          </div>
          <div className="my-6 text-center">
            <span className="text-5xl font-extrabold font-mono text-teal-400 tracking-tight">
              {metrics?.activeTasks ?? 0}
            </span>
          </div>
          <div className="text-xs text-slate-400 border-t border-slate-800 pt-3 flex justify-between">
            <span>Paused / Inactive:</span>
            <span className="font-mono text-slate-300 font-semibold">{metrics?.pausedTasks ?? 0}</span>
          </div>
        </div>
      </div>

      {/* Micrometer Metrics Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6">
        <h3 className="text-base font-semibold text-white mb-1">Custom Micrometer Counters & Timers</h3>
        <p className="text-xs text-slate-400 mb-6">Published dynamically by scheduler & worker daemons</p>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[
            { key: 'dts_tasks_scheduled_total', desc: 'Total tasks scanned & dispatched by leader scheduler' },
            { key: 'dts_tasks_executed_success', desc: 'HTTP / Webhook / Internal executions succeeding with 2xx' },
            { key: 'dts_tasks_retried_total', desc: 'Transient failures undergoing exponential backoff' },
            { key: 'dts_tasks_dlq_total', desc: 'Exhausted retry executions routed to Dead Letter Queue' },
            { key: 'dts_task_execution_duration', desc: 'Histogram timer measuring end-to-end task execution latency' },
            { key: 'dts_zombies_recovered_total', desc: 'Crashed worker tasks reaped & resurrected by watchdog' },
          ].map((item, idx) => (
            <div key={idx} className="p-4 bg-slate-950/70 border border-slate-800 rounded-lg">
              <span className="font-mono text-xs font-semibold text-emerald-400 block mb-1">
                {item.key}
              </span>
              <p className="text-xs text-slate-400">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
