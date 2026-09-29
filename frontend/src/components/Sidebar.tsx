import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  CalendarClock,
  PlaySquare,
  Cpu,
  BarChart3,
  ShieldAlert,
  PlusCircle,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const navItems = [
    { label: 'Dashboard', path: '/', icon: LayoutDashboard },
    { label: 'Scheduled Tasks', path: '/tasks', icon: CalendarClock },
    { label: 'Create Task', path: '/tasks/create', icon: PlusCircle },
    { label: 'Executions', path: '/executions', icon: PlaySquare },
    { label: 'Worker Fleet', path: '/workers', icon: Cpu },
    { label: 'System Metrics', path: '/metrics', icon: BarChart3 },
    { label: 'Audit Logs', path: '/audit-logs', icon: ShieldAlert },
  ];

  return (
    <aside className="w-64 bg-slate-900/80 border-r border-slate-800 flex flex-col h-screen fixed left-0 top-0 backdrop-blur-md z-30">
      <div className="h-16 flex items-center gap-3 px-6 border-b border-slate-800">
        <div className="w-9 h-9 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
          <CalendarClock className="w-5 h-5" />
        </div>
        <div>
          <span className="font-bold text-slate-100 tracking-tight text-base block">DTS Console</span>
          <span className="text-[10px] text-emerald-400 font-mono tracking-wider uppercase block font-semibold">Distributed Engine</span>
        </div>
      </div>

      <nav className="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                }`
              }
            >
              <Icon className="w-4 h-4 shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div className="p-4 border-t border-slate-800 bg-slate-950/40">
        <div className="flex items-center justify-between text-xs text-slate-400 mb-1">
          <span className="flex items-center gap-1.5 font-mono">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            CLUSTER ACTIVE
          </span>
          <span className="text-[10px] text-slate-500 font-mono">v1.0.0</span>
        </div>
        <p className="text-[11px] text-slate-500 leading-tight">
          Kafka • Redis Lock • Postgres ACID
        </p>
      </div>
    </aside>
  );
};
