import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { TaskType, ScheduleType, ConcurrencyPolicy, RetryPolicy, CreateTaskRequest } from '../types';
import {
  CalendarClock,
  ArrowLeft,
  Check,
  AlertCircle,
  HelpCircle,
  Globe,
  Radio,
  Cpu,
  ShieldAlert,
} from 'lucide-react';

export const TaskCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [taskType, setTaskType] = useState<TaskType>('HTTP_TASK');

  // HTTP / Webhook fields
  const [url, setUrl] = useState('https://httpbin.org/get');
  const [method, setMethod] = useState('GET');
  const [headers, setHeaders] = useState('{"Content-Type": "application/json"}');
  const [body, setBody] = useState('');

  // Internal Task field
  const [internalJob, setInternalJob] = useState('SYSTEM_HEALTH_CHECK');

  // Schedule fields
  const [scheduleType, setScheduleType] = useState<ScheduleType>('CRON');
  const [cronExpression, setCronExpression] = useState('0 */5 * * * *');
  const [intervalSeconds, setIntervalSeconds] = useState(60);
  const [scheduledAt, setScheduledAt] = useState('');
  const [timezone, setTimezone] = useState('UTC');

  // Concurrency & Retry fields
  const [concurrencyPolicy, setConcurrencyPolicy] = useState<ConcurrencyPolicy>('ALLOW_CONCURRENT');
  const [retryPolicy, setRetryPolicy] = useState<RetryPolicy>('EXPONENTIAL_BACKOFF');
  const [maxRetries, setMaxRetries] = useState(3);
  const [retryDelaySeconds, setRetryDelaySeconds] = useState(10);
  const [backoffMultiplier, setBackoffMultiplier] = useState(2.0);
  const [timeoutSeconds, setTimeoutSeconds] = useState(30);

  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (req: CreateTaskRequest) => api.createTask(req),
    onSuccess: (createdTask) => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['metrics'] });
      navigate(`/tasks/${createdTask.id}`);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || err.message || 'Failed to create task');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    let payloadString = '';
    if (taskType === 'HTTP_TASK' || taskType === 'WEBHOOK_TASK') {
      let parsedHeaders = {};
      try {
        if (headers.trim()) parsedHeaders = JSON.parse(headers);
      } catch {
        setError('Invalid JSON format for HTTP Headers');
        return;
      }

      payloadString = JSON.stringify({
        url: url.trim(),
        method,
        headers: parsedHeaders,
        body: body.trim() ? body.trim() : undefined,
      });
    } else {
      payloadString = internalJob;
    }

    const request: CreateTaskRequest = {
      name,
      description,
      taskType,
      payload: payloadString,
      scheduleType,
      cronExpression: scheduleType === 'CRON' ? cronExpression : undefined,
      intervalSeconds: scheduleType === 'INTERVAL' ? Number(intervalSeconds) : undefined,
      scheduledAt: scheduleType === 'ONE_TIME' && scheduledAt ? new Date(scheduledAt).toISOString() : undefined,
      timezone,
      concurrencyPolicy,
      retryPolicy,
      maxRetries: Number(maxRetries),
      retryDelaySeconds: Number(retryDelaySeconds),
      backoffMultiplier: Number(backoffMultiplier),
      maxRetryDelaySeconds: 300,
      timeoutSeconds: Number(timeoutSeconds),
      enabled: true,
    };

    mutation.mutate(request);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Link
          to="/tasks"
          className="p-2 text-slate-400 hover:text-white bg-slate-900 border border-slate-800 rounded-lg hover:border-slate-700 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <CalendarClock className="w-6 h-6 text-emerald-400" />
            Create Scheduled Task
          </h1>
          <p className="text-sm text-slate-400 mt-0.5">
            Configure job payload, trigger semantics, retry policies, and concurrency control
          </p>
        </div>
      </div>

      {error && (
        <div className="bg-rose-950/60 border border-rose-800/80 text-rose-300 text-sm rounded-xl p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Section 1: Basic Info */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 space-y-4">
          <h3 className="text-base font-semibold text-white border-b border-slate-800 pb-3">
            1. Task Identification
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Task Name *
              </label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Ingest Customer Invoices"
                className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Description
              </label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="e.g. Scrapes invoices and triggers webhook notification"
                className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>
          </div>
        </div>

        {/* Section 2: Task Type & Payload */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 space-y-5">
          <h3 className="text-base font-semibold text-white border-b border-slate-800 pb-3 flex items-center justify-between">
            <span>2. Task Type & Payload Configuration</span>
            <span className="text-xs font-normal text-slate-400 font-mono">Polymorphic Executors</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {[
              { type: 'HTTP_TASK', label: 'HTTP Task', desc: 'Direct REST endpoint invocation', icon: Globe },
              { type: 'WEBHOOK_TASK', label: 'Webhook Task', desc: 'Outbound POST with delivery headers', icon: Radio },
              { type: 'INTERNAL_TASK', label: 'Internal Task', desc: 'Pre-registered maintenance jobs', icon: Cpu },
            ].map((item) => {
              const Icon = item.icon;
              const isSelected = taskType === item.type;
              return (
                <div
                  key={item.type}
                  onClick={() => setTaskType(item.type as TaskType)}
                  className={`p-4 rounded-xl border cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-emerald-950/40 border-emerald-500 text-white ring-1 ring-emerald-500'
                      : 'bg-slate-950/70 border-slate-800 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1.5">
                    <Icon className={`w-4 h-4 ${isSelected ? 'text-emerald-400' : 'text-slate-500'}`} />
                    <span className="text-sm font-semibold">{item.label}</span>
                  </div>
                  <p className="text-xs text-slate-400 leading-snug">{item.desc}</p>
                </div>
              );
            })}
          </div>

          {(taskType === 'HTTP_TASK' || taskType === 'WEBHOOK_TASK') && (
            <div className="space-y-4 pt-2">
              <div className="p-3 bg-amber-950/30 border border-amber-800/50 rounded-lg flex items-center gap-2.5 text-xs text-amber-300">
                <ShieldAlert className="w-4 h-4 shrink-0 text-amber-400" />
                <span>SSRF Protection is active: Localhost, AWS metadata, and private IP ranges are rejected.</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
                <div className="sm:col-span-3">
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Target URL *
                  </label>
                  <input
                    type="url"
                    required
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    placeholder="https://api.example.com/webhook"
                    className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    HTTP Method
                  </label>
                  <select
                    value={method}
                    onChange={(e) => setMethod(e.target.value)}
                    className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:ring-1 focus:ring-emerald-500 font-mono"
                  >
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PUT">PUT</option>
                    <option value="DELETE">DELETE</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Request Headers (JSON)
                </label>
                <textarea
                  rows={2}
                  value={headers}
                  onChange={(e) => setHeaders(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-200 font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>

              {method !== 'GET' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Request Body (JSON / Text)
                  </label>
                  <textarea
                    rows={3}
                    value={body}
                    onChange={(e) => setBody(e.target.value)}
                    placeholder='{"key": "value"}'
                    className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-200 font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
              )}
            </div>
          )}

          {taskType === 'INTERNAL_TASK' && (
            <div className="pt-2">
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Internal Task Identifier
              </label>
              <select
                value={internalJob}
                onChange={(e) => setInternalJob(e.target.value)}
                className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
              >
                <option value="SYSTEM_HEALTH_CHECK">SYSTEM_HEALTH_CHECK (JVM memory & CPU audit)</option>
                <option value="MOCK_HEAVY_CALCULATION">MOCK_HEAVY_CALCULATION (Simulates compute workload)</option>
                <option value="MOCK_FAILURE_JOB">MOCK_FAILURE_JOB (Simulates intentional failure for retries)</option>
              </select>
            </div>
          )}
        </div>

        {/* Section 3: Schedule Configuration */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 space-y-4">
          <h3 className="text-base font-semibold text-white border-b border-slate-800 pb-3">
            3. Schedule & Trigger Semantics
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {[
              { type: 'CRON', label: 'Cron Expression', desc: 'UNIX / Quartz periodic schedule' },
              { type: 'INTERVAL', label: 'Fixed Interval', desc: 'Repeat every N seconds' },
              { type: 'ONE_TIME', label: 'One-Time Trigger', desc: 'Execute once at timestamp' },
            ].map((item) => (
              <div
                key={item.type}
                onClick={() => setScheduleType(item.type as ScheduleType)}
                className={`p-4 rounded-xl border cursor-pointer transition-all ${
                  scheduleType === item.type
                    ? 'bg-emerald-950/40 border-emerald-500 text-white ring-1 ring-emerald-500'
                    : 'bg-slate-950/70 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <span className="text-sm font-semibold block mb-1">{item.label}</span>
                <p className="text-xs text-slate-400">{item.desc}</p>
              </div>
            ))}
          </div>

          <div className="pt-2">
            {scheduleType === 'CRON' && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Cron Expression * (5 or 6 fields)
                  </label>
                  <input
                    type="text"
                    required
                    value={cronExpression}
                    onChange={(e) => setCronExpression(e.target.value)}
                    placeholder="0 */5 * * * *"
                    className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                  <p className="text-[11px] text-slate-500 mt-1">Examples: `0 */5 * * * *` (every 5 min), `0 0 * * * *` (hourly)</p>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Timezone
                  </label>
                  <input
                    type="text"
                    value={timezone}
                    onChange={(e) => setTimezone(e.target.value)}
                    placeholder="UTC or America/New_York"
                    className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
              </div>
            )}

            {scheduleType === 'INTERVAL' && (
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Interval Duration (Seconds) *
                </label>
                <input
                  type="number"
                  required
                  min={1}
                  value={intervalSeconds}
                  onChange={(e) => setIntervalSeconds(Number(e.target.value))}
                  className="w-full sm:w-1/2 px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>
            )}

            {scheduleType === 'ONE_TIME' && (
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Scheduled Execution Timestamp
                </label>
                <input
                  type="datetime-local"
                  value={scheduledAt}
                  onChange={(e) => setScheduledAt(e.target.value)}
                  className="w-full sm:w-1/2 px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>
            )}
          </div>
        </div>

        {/* Section 4: Concurrency & Retry Policies */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 space-y-4">
          <h3 className="text-base font-semibold text-white border-b border-slate-800 pb-3">
            4. Concurrency, Timeout & Retries
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Concurrency Policy
              </label>
              <select
                value={concurrencyPolicy}
                onChange={(e) => setConcurrencyPolicy(e.target.value as ConcurrencyPolicy)}
                className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:ring-1 focus:ring-emerald-500"
              >
                <option value="ALLOW_CONCURRENT">ALLOW_CONCURRENT (Permit overlapping executions)</option>
                <option value="FORBID_CONCURRENT">FORBID_CONCURRENT (Skip run if previous is active)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Execution Timeout (Seconds)
              </label>
              <input
                type="number"
                min={1}
                max={300}
                value={timeoutSeconds}
                onChange={(e) => setTimeoutSeconds(Number(e.target.value))}
                className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Max Retries Before Dead-Letter Queue (DLQ)
              </label>
              <input
                type="number"
                min={0}
                max={10}
                value={maxRetries}
                onChange={(e) => setMaxRetries(Number(e.target.value))}
                className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Initial Retry Delay (Seconds)
              </label>
              <input
                type="number"
                min={1}
                value={retryDelaySeconds}
                onChange={(e) => setRetryDelaySeconds(Number(e.target.value))}
                className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <Link
            to="/tasks"
            className="px-5 py-2.5 rounded-lg border border-slate-800 text-sm font-semibold text-slate-400 hover:text-white hover:bg-slate-900 transition-colors"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-6 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-semibold transition-colors shadow-sm disabled:opacity-50"
          >
            {mutation.isPending ? 'Registering Task...' : 'Schedule Task'}
          </button>
        </div>
      </form>
    </div>
  );
};
