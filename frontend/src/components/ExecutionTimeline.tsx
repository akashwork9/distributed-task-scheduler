import React from 'react';
import { ExecutionStatus } from '../types';
import { CheckCircle2, Clock, PlayCircle, AlertOctagon, RefreshCw, XCircle } from 'lucide-react';

interface ExecutionTimelineProps {
  status: ExecutionStatus;
  attempt: number;
  scheduledAt: string;
  startedAt?: string;
  completedAt?: string;
}

export const ExecutionTimeline: React.FC<ExecutionTimelineProps> = ({
  status,
  attempt,
  scheduledAt,
  startedAt,
  completedAt,
}) => {
  const steps = [
    {
      id: 'SCHEDULED',
      label: 'Scheduled',
      icon: Clock,
      completed: true,
      time: scheduledAt,
    },
    {
      id: 'QUEUED',
      label: 'Queued to Kafka',
      icon: Clock,
      completed: status !== 'SCHEDULED',
      time: scheduledAt,
    },
    {
      id: 'RUNNING',
      label: startedAt ? `Running (Attempt #${attempt})` : 'Awaiting Worker',
      icon: PlayCircle,
      completed: ['RUNNING', 'RETRYING', 'SUCCESS', 'FAILED'].includes(status),
      current: status === 'RUNNING',
      time: startedAt,
    },
    ...(attempt > 1 || status === 'RETRYING'
      ? [
          {
            id: 'RETRYING',
            label: `Retrying (Attempt #${attempt})`,
            icon: RefreshCw,
            completed: ['SUCCESS', 'FAILED'].includes(status),
            current: status === 'RETRYING',
            time: undefined,
          },
        ]
      : []),
    {
      id: 'TERMINAL',
      label:
        status === 'SUCCESS'
          ? 'Completed Successfully'
          : status === 'FAILED'
          ? 'Failed (DLQ Exhausted)'
          : status === 'CANCELLED'
          ? 'Cancelled'
          : 'Pending Completion',
      icon: status === 'SUCCESS' ? CheckCircle2 : status === 'FAILED' ? AlertOctagon : status === 'CANCELLED' ? XCircle : Clock,
      completed: ['SUCCESS', 'FAILED', 'CANCELLED'].includes(status),
      current: ['SUCCESS', 'FAILED', 'CANCELLED'].includes(status),
      time: completedAt,
      isError: status === 'FAILED',
      isSuccess: status === 'SUCCESS',
    },
  ];

  return (
    <div className="w-full py-4">
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 relative">
        <div className="hidden md:block absolute top-1/2 left-0 right-0 h-0.5 bg-slate-800 -translate-y-1/2 z-0" />
        {steps.map((step, idx) => {
          const Icon = step.icon;
          let nodeColor = 'bg-slate-900 border-slate-700 text-slate-500';
          if (step.isSuccess) {
            nodeColor = 'bg-emerald-950 border-emerald-500 text-emerald-400 ring-4 ring-emerald-950/50';
          } else if (step.isError) {
            nodeColor = 'bg-rose-950 border-rose-500 text-rose-400 ring-4 ring-rose-950/50';
          } else if (step.current) {
            nodeColor = 'bg-blue-950 border-blue-500 text-blue-400 animate-pulse ring-4 ring-blue-950/50';
          } else if (step.completed) {
            nodeColor = 'bg-slate-900 border-emerald-500 text-emerald-400';
          }

          return (
            <div key={idx} className="flex md:flex-col items-center gap-3 md:gap-2 z-10 text-left md:text-center">
              <div className={`w-10 h-10 rounded-full border-2 flex items-center justify-center transition-all ${nodeColor}`}>
                <Icon className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-200">{step.label}</p>
                {step.time && (
                  <p className="text-xs text-slate-400 font-mono">
                    {new Date(step.time).toLocaleTimeString()}
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
