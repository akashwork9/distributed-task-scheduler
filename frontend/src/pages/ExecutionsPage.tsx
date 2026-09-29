import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { StatusBadge } from '../components/StatusBadge';
import { Link } from 'react-router-dom';
import { PlaySquare, RefreshCw, ExternalLink, Filter } from 'lucide-react';

export const ExecutionsPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['executions', page],
    queryFn: () => api.getAllExecutions(page, 20),
    refetchInterval: 5000,
  });

  const retryMutation = useMutation({
    mutationFn: (id: number) => api.retryExecution(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['executions'] });
      queryClient.invalidateQueries({ queryKey: ['metrics'] });
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <PlaySquare className="w-6 h-6 text-emerald-400" />
            Execution History & DLQ
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Global audit log of distributed task executions, retries, and dead-letter queue records
          </p>
        </div>
      </div>

      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="px-6 py-3.5">Execution ID</th>
                <th className="px-6 py-3.5">Task Name</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Attempt</th>
                <th className="px-6 py-3.5">Worker</th>
                <th className="px-6 py-3.5">Duration</th>
                <th className="px-6 py-3.5">Scheduled At</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {data?.content.map((exec) => (
                <tr key={exec.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="px-6 py-4 font-mono text-xs text-slate-400">
                    <Link
                      to={`/executions/${exec.id}`}
                      className="text-emerald-400 hover:underline hover:text-emerald-300 font-semibold"
                    >
                      {exec.executionId.substring(0, 16)}...
                    </Link>
                  </td>
                  <td className="px-6 py-4 font-medium text-slate-200">
                    <Link to={`/tasks/${exec.taskId}`} className="hover:underline">
                      {exec.taskName}
                    </Link>
                  </td>
                  <td className="px-6 py-4">
                    <StatusBadge status={exec.status} size="sm" />
                  </td>
                  <td className="px-6 py-4 font-mono text-xs">
                    <span className="bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
                      Attempt #{exec.attempt}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-mono text-xs text-slate-400">
                    {exec.workerId || '—'}
                  </td>
                  <td className="px-6 py-4 font-mono text-xs">
                    {exec.durationMs ? `${exec.durationMs}ms` : '—'}
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-400">
                    {new Date(exec.scheduledAt).toLocaleString()}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      {(exec.status === 'FAILED' || exec.status === 'CANCELLED') && (
                        <button
                          onClick={() => retryMutation.mutate(exec.id)}
                          disabled={retryMutation.isPending}
                          title="Retry execution manually"
                          className="px-2.5 py-1 text-xs font-semibold bg-emerald-950/60 hover:bg-emerald-900/80 text-emerald-400 border border-emerald-800/80 rounded-lg transition-colors flex items-center gap-1.5"
                        >
                          <RefreshCw className="w-3.5 h-3.5" />
                          <span>Retry</span>
                        </button>
                      )}
                      <Link
                        to={`/executions/${exec.id}`}
                        className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
                        title="View Execution Details"
                      >
                        <ExternalLink className="w-4 h-4" />
                      </Link>
                    </div>
                  </td>
                </tr>
              ))}

              {(!data || data.content.length === 0) && !isLoading && (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-slate-500">
                    No executions recorded yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="px-6 py-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>
              Page {data.pageNumber + 1} of {data.totalPages} ({data.totalElements} executions)
            </span>
            <div className="flex items-center gap-2">
              <button
                disabled={data.isFirst}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1 bg-slate-950 border border-slate-800 rounded text-slate-300 disabled:opacity-40"
              >
                Previous
              </button>
              <button
                disabled={data.isLast}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 bg-slate-950 border border-slate-800 rounded text-slate-300 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
