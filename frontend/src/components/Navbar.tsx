import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User as UserIcon, Shield, Radio } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 bg-slate-900/60 border-b border-slate-800 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-20">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2 bg-emerald-950/40 border border-emerald-800/50 px-2.5 py-1 rounded-md text-emerald-400 text-xs font-mono font-medium">
          <Radio className="w-3.5 h-3.5 animate-pulse" />
          <span>Multi-Scheduler Replicas Online</span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {user && (
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300">
                <UserIcon className="w-4 h-4" />
              </div>
              <div className="text-left hidden sm:block">
                <p className="text-xs font-medium text-slate-200">{user.name}</p>
                <p className="text-[10px] text-slate-400 font-mono">{user.email}</p>
              </div>
            </div>

            {user.role === 'ROLE_ADMIN' ? (
              <span className="inline-flex items-center gap-1 bg-amber-950/60 border border-amber-800/80 text-amber-400 text-[10px] font-bold px-2 py-0.5 rounded uppercase">
                <Shield className="w-3 h-3" /> ADMIN
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 bg-slate-800 border border-slate-700 text-slate-400 text-[10px] font-medium px-2 py-0.5 rounded uppercase">
                USER
              </span>
            )}

            <button
              onClick={logout}
              title="Sign Out"
              className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
