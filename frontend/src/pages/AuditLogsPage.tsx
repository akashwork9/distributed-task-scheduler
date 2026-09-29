import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { ShieldAlert, User, Clock, Terminal } from 'lucide-react';

export const AuditLogsPage: React.FC = () => {
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['audit-logs', page],
    queryFn: () => api.getAuditLogs(page, 20),
    refetchInterval: 6000,
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <ShieldAlert className="w-6 h-6 text-amber-400" />
            Security & Operational Audit Log
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Immutable ledger of administrative mutations, execution claims, and scheduling decisions
          </p>
        </div>
      </div>

      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="px-6 py-3.5">Timestamp</th>
                <th className="px-6 py-3.5">Action</th>
                <th className="px-6 py-3.5">User</th>
                <th className="px-6 py-3.5">Entity</th>
                <th className="px-6 py-3.5">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {data?.content.map((log) => (
                <tr key={log.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="px-6 py-4 font-mono text-xs text-slate-400 whitespace-nowrap">
                    {new Date(log.createdAt).toLocaleString()}
                  </td>
                  <td className="px-6 py-4">
                    <span className="font-mono text-xs font-semibold bg-slate-950 px-2 py-0.5 rounded border border-slate-800 text-amber-400">
                      {log.action}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-xs font-medium text-slate-300">
                    {log.userEmail}
                  </td>
                  <td className="px-6 py-4 font-mono text-xs text-slate-400">
                    {log.entityType} #{log.entityId}
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-300">
                    {log.metadata || '—'}
                  </td>
                </tr>
              ))}

              {(!data || data.content.length === 0) && !isLoading && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-slate-500">
                    No audit records logged yet.
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
              Page {data.pageNumber + 1} of {data.totalPages} ({data.totalElements} records)
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
