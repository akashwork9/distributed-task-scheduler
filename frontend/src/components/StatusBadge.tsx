import React from 'react';
import { TaskStatus, ExecutionStatus, WorkerStatus } from '../types';

interface StatusBadgeProps {
  status: TaskStatus | ExecutionStatus | WorkerStatus | string;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  let colorClasses = 'bg-slate-800 text-slate-400 border-slate-700';

  switch (status) {
    case 'ACTIVE':
    case 'SUCCESS':
      colorClasses = 'bg-emerald-950/60 text-emerald-400 border-emerald-800/80';
      break;
    case 'RUNNING':
      colorClasses = 'bg-blue-950/60 text-blue-400 border-blue-800/80 animate-pulse';
      break;
    case 'QUEUED':
    case 'SCHEDULED':
      colorClasses = 'bg-amber-950/60 text-amber-400 border-amber-800/80';
      break;
    case 'RETRYING':
      colorClasses = 'bg-orange-950/60 text-orange-400 border-orange-800/80';
      break;
    case 'FAILED':
    case 'OFFLINE':
      colorClasses = 'bg-rose-950/60 text-rose-400 border-rose-800/80';
      break;
    case 'PAUSED':
    case 'UNHEALTHY':
    case 'CANCELLED':
      colorClasses = 'bg-zinc-800/70 text-zinc-400 border-zinc-700';
      break;
  }

  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs font-medium';

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border ${sizeClasses} ${colorClasses} tracking-wide uppercase font-semibold`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current opacity-75"></span>
      {status}
    </span>
  );
};
